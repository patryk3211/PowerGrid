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

public class ElectronTubeWire extends CompoundWire implements ISolverHook {
    private static final double GRID_LEAK_CONDUCTANCE = 1e-6;
    private static final double GRID_PERVEANCE = 5e-5;

    private final IElectricNode grid;

    private final float mu;
    private final float kg1;
    private final float kp;
    private final float kvb;
    private final float ex;
    private float saturationCurrent;

    private double prevGrid;
    private double prevCathode;
    private double prevAnode;

    private double linearizedAnodeCurrent;
    private double linearizedGridCurrent;
    private double reportedCurrent;
    private double reportedPower;

    private final ConductanceWire gridCathode;
    private final GMStamp gmStamp;

    public ElectronTubeWire(float mu, float kg1, float kp, float kvb, float ex, float saturationCurrent, IElectricNode cathode, IElectricNode anode, IElectricNode grid) {
        super(cathode, anode);
        this.grid = grid;
        gridCathode = addDynamicWire(grid, cathode);
        gmStamp = addInternalWire(new GMStamp(cathode, anode, grid));

        this.mu = mu;
        this.kg1 = kg1;
        this.kp = kp;
        this.kvb = kvb;
        this.ex = ex;
        this.saturationCurrent = saturationCurrent;
    }

    @Override
    public Collection<IElectricNode> coupledNodes() {
        return List.of(node1, node2, grid);
    }

    public void setSaturationCurrent(float saturationCurrent) {
        valueChange(saturationCurrent, this.saturationCurrent);
        this.saturationCurrent = saturationCurrent;
    }

    @Override
    public void startIteration(int iteration) {
        // Norman Koren triode model, http://www.normankoren.com/Audio/Tubemodspice_article.html
        double vCathode = node1.getVoltage();
        double vGrid = grid.getVoltage();
        double vAnode = node2.getVoltage();
        var cathodeStep = vCathode - prevCathode;
        var gridStep = vGrid - prevGrid;
        var anodeStep = vAnode - prevAnode;
        vCathode = prevCathode + Math.min(0.5f, Math.abs(cathodeStep)) * network.triodeLimCathode * Math.signum(cathodeStep);
        vGrid = prevGrid + Math.min(0.5f, Math.abs(gridStep)) * network.triodeLimGrid * Math.signum(gridStep);
        vAnode = prevAnode + Math.min(0.5f, Math.abs(anodeStep)) * network.triodeLimAnode * Math.signum(anodeStep);
        prevAnode = vAnode;
        prevCathode = vCathode;
        prevGrid = vGrid;
        vGrid -= vCathode;
        vAnode -= vCathode;

        var plate = evaluatePlate(vAnode, vGrid);
        var gridEval = evaluateGrid(vGrid);

        double anodeConductance = plate.dCurrent_dAnode;
        double transconductance = plate.dCurrent_dGrid;
        double anodeCurrent = plate.current;

        linearizedAnodeCurrent = -anodeCurrent + anodeConductance * vAnode + transconductance * vGrid;
        linearizedGridCurrent = -gridEval.current + gridEval.conductance * vGrid;

        var actualVAnode = node2.getVoltage() - node1.getVoltage();
        var actualVGrid = grid.getVoltage() - node1.getVoltage();
        var actualAnodeCurrent = evaluatePlateCurrent(actualVAnode, actualVGrid);
        var actualGridCurrent = evaluateGridCurrent(actualVGrid);
        reportedCurrent = actualAnodeCurrent + actualGridCurrent;
        reportedPower = actualAnodeCurrent * actualVAnode + actualGridCurrent * actualVGrid;

        gridCathode.setConductance(gridEval.conductance);
        setConductance(anodeConductance);
        gmStamp.setConductance(transconductance);
    }

    private record PlateState(double current, double dCurrent_dAnode, double dCurrent_dGrid) {}

    private PlateState evaluatePlate(double vAnode, double vGrid) {
        if (vAnode <= 0 || saturationCurrent == 0)
            return new PlateState(vAnode * ElectricalNetwork.G_MIN, ElectricalNetwork.G_MIN, 0);

        var e1State = evaluateE1(vAnode, vGrid);
        if (e1State.e1 < 0)
            return new PlateState(0, 0, 0);

        var rawCurrent = korenPlateFactor(e1State.e1) * Math.pow(e1State.e1, ex) / kg1;
        var dRaw_dE1 = korenPlateFactor(e1State.e1) * ex * Math.pow(e1State.e1, ex - 1) / kg1;
        var saturated = applySaturation(rawCurrent, dRaw_dE1);

        return new PlateState(
                saturated.current,
                saturated.dCurrent_dRaw * e1State.dE1_dAnode,
                saturated.dCurrent_dRaw * e1State.dE1_dGrid
        );
    }

    private record E1State(double e1, double dE1_dAnode, double dE1_dGrid) {}

    private E1State evaluateE1(double vAnode, double vGrid) {
        var kneeTerm = Math.sqrt(kvb + vAnode * vAnode);
        var driveTerm = kp * (1 / mu + vGrid / kneeTerm);
        var sp = softplus(driveTerm);
        var e1 = vAnode / kp * sp;
        var sigmoid = 1 / (1 + Math.exp(-driveTerm));
        var dE1_dAnode = sp / kp + vAnode / kp * sigmoid * (-kp * vGrid * vAnode / Math.pow(kneeTerm, 3));
        var dE1_dGrid = vAnode * sigmoid / kneeTerm;
        return new E1State(e1, dE1_dAnode, dE1_dGrid);
    }

    private record SaturatedCurrent(double current, double dCurrent_dRaw) {}

    private SaturatedCurrent applySaturation(double rawCurrent, double dRaw_dE1) {
        if (saturationCurrent <= 0)
            return new SaturatedCurrent(rawCurrent, dRaw_dE1);

        var ratio = rawCurrent / saturationCurrent;
        var denom = Math.sqrt(Math.sqrt(1 + ratio * ratio));
        var current = rawCurrent / denom;
        var dDenom_dRaw = 0.25 * Math.pow(1 + ratio * ratio, -0.75) * 2 * ratio / saturationCurrent;
        var dCurrent_dRaw = (dRaw_dE1 * denom - rawCurrent * dDenom_dRaw * dRaw_dE1) / (denom * denom);
        return new SaturatedCurrent(current, dCurrent_dRaw);
    }

    private static double korenPlateFactor(double e1) {
        return e1 >= 0 ? 2 : 0;
    }

    private record GridState(double current, double conductance) {}

    private GridState evaluateGrid(double vGrid) {
        var leak = GRID_LEAK_CONDUCTANCE * vGrid;
        var vgPos = positiveGridVoltage(vGrid);
        var forward = (2.0 / 3.0) * GRID_PERVEANCE * Math.pow(vgPos, 1.5);
        var dForward = GRID_PERVEANCE * Math.sqrt(Math.max(vgPos, 1e-12)) * positiveGridDerivative(vGrid);
        var conductance = Math.max(GRID_LEAK_CONDUCTANCE + dForward, ElectricalNetwork.G_MIN);
        return new GridState(leak + forward, conductance);
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

    private double evaluatePlateCurrent(double vAnode, double vGrid) {
        return evaluatePlate(vAnode, vGrid).current;
    }

    private double evaluateGridCurrent(double vGrid) {
        return evaluateGrid(vGrid).current;
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
        residual.add(node1.getIndex(), linearizedAnodeCurrent + linearizedGridCurrent);
        residual.add(node2.getIndex(), -linearizedAnodeCurrent);
        residual.add(grid.getIndex(), -linearizedGridCurrent);
    }

    @Override
    public double current() {
        return reportedCurrent;
    }

    @Override
    public double internalPower() {
        return reportedPower;
    }

    public static float calculateKg1(float anodeVoltage, float gridVoltage, float mu, float kp, float kvb, float ex, float anodeCurrent) {
        if (anodeCurrent <= 0 || anodeVoltage <= 0)
            return Float.POSITIVE_INFINITY;

        var kneeTerm = Math.sqrt(kvb + anodeVoltage * anodeVoltage);
        var driveTerm = kp * (1 / mu + gridVoltage / kneeTerm);
        var e1 = anodeVoltage / kp * softplus(driveTerm);
        if (e1 < 0)
            return Float.POSITIVE_INFINITY;

        var factor = korenPlateFactor(e1);
        return (float) (factor * Math.pow(e1, ex) / anodeCurrent);
    }

    @Override
    public String toString() {
        return String.format("ElectronTube(mu=%g Kg1=%g Kp=%g Kvb=%g Ex=%g Is=%g)", mu, kg1, kp, kvb, ex, saturationCurrent);
    }

    private static class GMStamp extends ConductanceWire {
        private final IElectricNode node3;

        public GMStamp(IElectricNode node1, IElectricNode node2, IElectricNode node3) {
            super(node1, node2);
            this.node3 = node3;
        }

        @Override
        public void stamp(IAdmittanceAdder admittance, double change) {
            // Anode-Cathode
            admittance.add(node2.getIndex(), node1.getIndex(), -change);
            // Anode-Grid
            admittance.add(node2.getIndex(), node3.getIndex(), change);
            // Cathode-Cathode
            admittance.add(node1.getIndex(), node1.getIndex(), change);
            // Cathode-Grid
            admittance.add(node1.getIndex(), node3.getIndex(), -change);
        }

        @Override
        public List<IElectricNode> coupledNodes() {
            return List.of(node1, node2, node3);
        }

        @Override
        public String toString() {
            return String.format("ElectronTube#GM(gm=%g)", conductance());
        }
    }
}
