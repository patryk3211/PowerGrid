/*
 * Copyright 2026 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.electricity.sim.special;

import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IAdmittanceAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

import java.util.Collection;
import java.util.List;

public class PentodeWire extends CompoundWire implements ISolverHook {
    private static final double GRID_LEAK_CONDUCTANCE = 1e-6;
    private static final double GRID_PERVEANCE = 5e-5;
    private static final double SCREEN_FLOOR = 1e-3;

    private final IElectricNode grid;
    private final IElectricNode screen;

    private final float mu;
    private final float kg1;
    private final float kg2;
    private final float kp;
    private final float kvb;
    private final float ex;
    private float saturationCurrent;

    private double prevGrid;
    private double prevScreen;
    private double prevCathode;
    private double prevAnode;

    private double linearizedAnodeCurrent;
    private double linearizedGridCurrent;
    private double linearizedScreenCurrent;
    private double reportedCurrent;
    private double reportedPower;

    private final ConductanceWire gridCathode;
    private final ConductanceWire screenCathode;
    private final GMStamp gmPlateGrid;
    private final GMStamp gmPlateScreen;
    private final GMStamp gmScreenGrid;

    public PentodeWire(float mu, float kg1, float kg2, float kp, float kvb, float ex, float saturationCurrent,
                       IElectricNode cathode, IElectricNode anode, IElectricNode grid, IElectricNode screen) {
        super(cathode, anode);
        this.grid = grid;
        this.screen = screen;
        gridCathode = addDynamicWire(grid, cathode);
        screenCathode = addDynamicWire(screen, cathode);
        gmPlateGrid = addInternalWire(new GMStamp(cathode, anode, grid));
        gmPlateScreen = addInternalWire(new GMStamp(cathode, anode, screen));
        gmScreenGrid = addInternalWire(new GMStamp(cathode, screen, grid));

        this.mu = mu;
        this.kg1 = kg1;
        this.kg2 = kg2;
        this.kp = kp;
        this.kvb = kvb;
        this.ex = ex;
        this.saturationCurrent = saturationCurrent;
    }

    @Override
    public Collection<IElectricNode> coupledNodes() {
        return List.of(node1, node2, grid, screen);
    }

    public void setSaturationCurrent(float saturationCurrent) {
        valueChange(saturationCurrent, this.saturationCurrent);
        this.saturationCurrent = saturationCurrent;
    }

    @Override
    public void startIteration(int iteration) {
        double vCathode = node1.getVoltage();
        double vGrid = grid.getVoltage();
        double vScreen = screen.getVoltage();
        double vAnode = node2.getVoltage();

        var cathodeStep = vCathode - prevCathode;
        var gridStep = vGrid - prevGrid;
        var screenStep = vScreen - prevScreen;
        var anodeStep = vAnode - prevAnode;
        vCathode = prevCathode + Math.min(0.5f, Math.abs(cathodeStep)) * network.triodeLimCathode * Math.signum(cathodeStep);
        vGrid = prevGrid + Math.min(0.5f, Math.abs(gridStep)) * network.triodeLimGrid * Math.signum(gridStep);
        vScreen = prevScreen + Math.min(0.5f, Math.abs(screenStep)) * network.triodeLimGrid * Math.signum(screenStep);
        vAnode = prevAnode + Math.min(0.5f, Math.abs(anodeStep)) * network.triodeLimAnode * Math.signum(anodeStep);
        prevAnode = vAnode;
        prevCathode = vCathode;
        prevGrid = vGrid;
        prevScreen = vScreen;

        vGrid -= vCathode;
        vScreen -= vCathode;
        vAnode -= vCathode;

        var plate = evaluatePlate(vAnode, vGrid, vScreen);
        var gridEval = evaluateControlGrid(vGrid);
        var screenEval = evaluateScreen(vGrid, vScreen);

        linearizedAnodeCurrent = -plate.current + plate.dCurrent_dAnode * vAnode
                + plate.dCurrent_dGrid * vGrid + plate.dCurrent_dScreen * vScreen;
        linearizedGridCurrent = -gridEval.current + gridEval.conductance * vGrid;
        linearizedScreenCurrent = -screenEval.current
                + screenEval.dCurrent_dScreen * vScreen + screenEval.dCurrent_dGrid * vGrid;

        var actualVAnode = node2.getVoltage() - node1.getVoltage();
        var actualVGrid = grid.getVoltage() - node1.getVoltage();
        var actualVScreen = screen.getVoltage() - node1.getVoltage();
        var actualAnodeCurrent = evaluatePlate(actualVAnode, actualVGrid, actualVScreen).current;
        var actualGridCurrent = evaluateControlGrid(actualVGrid).current;
        var actualScreenCurrent = evaluateScreen(actualVGrid, actualVScreen).current;
        reportedCurrent = actualAnodeCurrent + actualGridCurrent + actualScreenCurrent;
        reportedPower = actualAnodeCurrent * actualVAnode
                + actualGridCurrent * actualVGrid
                + actualScreenCurrent * actualVScreen;

        gridCathode.setConductance(gridEval.conductance);
        screenCathode.setConductance(Math.max(screenEval.dCurrent_dScreen, ElectricalNetwork.G_MIN));
        setConductance(plate.dCurrent_dAnode);
        gmPlateGrid.setConductance(plate.dCurrent_dGrid);
        gmPlateScreen.setConductance(plate.dCurrent_dScreen);
        gmScreenGrid.setConductance(screenEval.dCurrent_dGrid);
    }

    private record PlateState(double current, double dCurrent_dAnode, double dCurrent_dGrid, double dCurrent_dScreen) {}

    private PlateState evaluatePlate(double vAnode, double vGrid, double vScreen) {
        if (vAnode <= 0 || vScreen <= 0 || saturationCurrent == 0)
            return new PlateState(vAnode * ElectricalNetwork.G_MIN, ElectricalNetwork.G_MIN, 0, 0);

        var e1State = evaluateE1(vGrid, vScreen);
        if (e1State.e1 < 0)
            return new PlateState(0, 0, 0, 0);

        var knee = Math.atan(vAnode / kvb);
        var dKnee_dAnode = (1.0 / kvb) / (1.0 + (vAnode / kvb) * (vAnode / kvb));
        var base = korenPlateFactor(e1State.e1) * Math.pow(e1State.e1, ex) / kg1;
        var dBase_dE1 = korenPlateFactor(e1State.e1) * ex * Math.pow(e1State.e1, ex - 1) / kg1;

        var rawCurrent = base * knee;
        var dRaw_dE1 = dBase_dE1 * knee;
        var dRaw_dAnode = base * dKnee_dAnode;
        var saturated = applySaturation(rawCurrent, 1);

        return new PlateState(
                saturated.current,
                saturated.dCurrent_dRaw * dRaw_dAnode,
                saturated.dCurrent_dRaw * dRaw_dE1 * e1State.dE1_dGrid,
                saturated.dCurrent_dRaw * dRaw_dE1 * e1State.dE1_dScreen
        );
    }

    private record E1State(double e1, double dE1_dGrid, double dE1_dScreen) {}

    private E1State evaluateE1(double vGrid, double vScreen) {
        var vs = Math.max(vScreen, SCREEN_FLOOR);
        var driveTerm = kp * (1 / mu + vGrid / vs);
        var sp = softplus(driveTerm);
        var e1 = vs / kp * sp;
        var sigmoid = sigmoid(driveTerm);
        var dE1_dGrid = sigmoid;
        var dE1_dScreen = sp / kp - sigmoid * vGrid / (vs * vs);
        return new E1State(e1, dE1_dGrid, dE1_dScreen);
    }

    private record SaturatedCurrent(double current, double dCurrent_dRaw) {}

    private SaturatedCurrent applySaturation(double rawCurrent, double dRaw_dInput) {
        if (saturationCurrent <= 0)
            return new SaturatedCurrent(rawCurrent, dRaw_dInput);

        var ratio = rawCurrent / saturationCurrent;
        var denom = Math.sqrt(Math.sqrt(1 + ratio * ratio));
        var current = rawCurrent / denom;
        var dDenom_dRaw = 0.25 * Math.pow(1 + ratio * ratio, -0.75) * 2 * ratio / saturationCurrent;
        var dCurrent_dRaw = (dRaw_dInput * denom - rawCurrent * dDenom_dRaw * dRaw_dInput) / (denom * denom);
        return new SaturatedCurrent(current, dCurrent_dRaw);
    }

    private static double korenPlateFactor(double e1) {
        return e1 >= 0 ? 2 : 0;
    }

    private record GridState(double current, double conductance) {}

    private GridState evaluateControlGrid(double vGrid) {
        var leak = GRID_LEAK_CONDUCTANCE * vGrid;
        var vgPos = positiveGridVoltage(vGrid);
        var forward = (2.0 / 3.0) * GRID_PERVEANCE * Math.pow(vgPos, 1.5);
        var dForward = GRID_PERVEANCE * Math.sqrt(Math.max(vgPos, 1e-12)) * positiveGridDerivative(vGrid);
        var conductance = Math.max(GRID_LEAK_CONDUCTANCE + dForward, ElectricalNetwork.G_MIN);
        return new GridState(leak + forward, conductance);
    }

    private record ScreenState(double current, double dCurrent_dGrid, double dCurrent_dScreen) {}

    private ScreenState evaluateScreen(double vGrid, double vScreen) {
        var drive = vGrid + vScreen / mu;
        var drivePos = softplus(drive);
        if (drivePos <= 0)
            return new ScreenState(0, 0, ElectricalNetwork.G_MIN);

        var current = Math.pow(drivePos, 1.5) / kg2;
        var dCurrent_dDrive = 1.5 * Math.sqrt(drivePos) / kg2 * sigmoid(drive);
        return new ScreenState(current, dCurrent_dDrive, dCurrent_dDrive / mu);
    }

    private static double positiveGridVoltage(double vGrid) {
        if (vGrid <= 0)
            return softplus(vGrid * 10) / 10;
        return vGrid;
    }

    private static double positiveGridDerivative(double vGrid) {
        if (vGrid > 0)
            return 1;
        return sigmoid(vGrid * 10);
    }

    private static double sigmoid(double x) {
        if (x > 20)
            return 1;
        if (x < -20)
            return 0;
        return 1 / (1 + Math.exp(-x));
    }

    private static double softplus(double x) {
        if (x > 20)
            return x;
        if (x < -20)
            return 0;
        return Math.log1p(Math.exp(x));
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        residual.add(node1.getIndex(), linearizedAnodeCurrent + linearizedGridCurrent + linearizedScreenCurrent);
        residual.add(node2.getIndex(), -linearizedAnodeCurrent);
        residual.add(grid.getIndex(), -linearizedGridCurrent);
        residual.add(screen.getIndex(), -linearizedScreenCurrent);
    }

    @Override
    public double current() {
        return reportedCurrent;
    }

    @Override
    public double internalPower() {
        return reportedPower;
    }

    @Override
    public String toString() {
        return String.format("Pentode(mu=%g Kg1=%g Kg2=%g Kp=%g Kvb=%g Ex=%g Is=%g)",
                mu, kg1, kg2, kp, kvb, ex, saturationCurrent);
    }

    private static class GMStamp extends ConductanceWire {
        private final IElectricNode node3;

        public GMStamp(IElectricNode node1, IElectricNode node2, IElectricNode node3) {
            super(node1, node2);
            this.node3 = node3;
        }

        @Override
        public void stamp(IAdmittanceAdder admittance, double change) {
            admittance.add(node2.getIndex(), node1.getIndex(), -change);
            admittance.add(node2.getIndex(), node3.getIndex(), change);
            admittance.add(node1.getIndex(), node1.getIndex(), change);
            admittance.add(node1.getIndex(), node3.getIndex(), -change);
        }

        @Override
        public List<IElectricNode> coupledNodes() {
            return List.of(node1, node2, node3);
        }

        @Override
        public String toString() {
            return String.format("Pentode#GM(gm=%g)", conductance());
        }
    }
}
