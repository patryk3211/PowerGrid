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
    private static final double GRID_CONDUCTANCE = 1e-6;

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

        gridCathode.setConductance(GRID_CONDUCTANCE);

        double anodeCurrent, anodeConductance, transconductance = 0;
        if (vAnode <= 0 || saturationCurrent == 0) {
            anodeConductance = ElectricalNetwork.G_MIN;
            anodeCurrent = vAnode * anodeConductance;
        } else {
            var kneeTerm = Math.sqrt(kvb + vAnode * vAnode);
            var driveTerm = kp * (1 / mu + vGrid / kneeTerm);
            var softplus = softplus(driveTerm);
            var e1 = vAnode / kp * softplus;

            if (e1 <= 0) {
                anodeConductance = ElectricalNetwork.G_MIN;
                anodeCurrent = vAnode * anodeConductance;
            } else {
                anodeCurrent = Math.pow(e1, ex) / kg1;

                if (saturationCurrent > 0) {
                    var saturationRatio = anodeCurrent / saturationCurrent;
                    anodeCurrent /= Math.sqrt(Math.sqrt(1 + saturationRatio * saturationRatio));
                }

                var sigmoid = 1 / (1 + Math.exp(-driveTerm));
                var dE1_dAnode = softplus / kp + vAnode / kp * sigmoid * (-kp * vGrid * vAnode / Math.pow(kneeTerm, 3));
                var dE1_dGrid = vAnode * sigmoid / kneeTerm;
                var dAnodeCurrent_dE1 = ex * Math.pow(e1, ex - 1) / kg1;

                anodeConductance = dAnodeCurrent_dE1 * dE1_dAnode;
                transconductance = dAnodeCurrent_dE1 * dE1_dGrid;
            }
        }

        linearizedAnodeCurrent = -anodeCurrent + anodeConductance * vAnode + transconductance * vGrid;

        var actualVAnode = node2.getVoltage() - node1.getVoltage();
        var actualVGrid = grid.getVoltage() - node1.getVoltage();
        var actualAnodeCurrent = evaluatePlateCurrent(actualVAnode, actualVGrid);
        var actualGridCurrent = GRID_CONDUCTANCE * actualVGrid;
        reportedCurrent = actualAnodeCurrent + actualGridCurrent;
        reportedPower = actualAnodeCurrent * actualVAnode + actualGridCurrent * actualVGrid;

        // Anode-Cathode "wire"
        setConductance(anodeConductance);
        gmStamp.setConductance(transconductance);
    }

    private double evaluatePlateCurrent(double vAnode, double vGrid) {
        if (vAnode <= 0 || saturationCurrent == 0)
            return vAnode * ElectricalNetwork.G_MIN;

        var kneeTerm = Math.sqrt(kvb + vAnode * vAnode);
        var driveTerm = kp * (1 / mu + vGrid / kneeTerm);
        var e1 = vAnode / kp * softplus(driveTerm);

        if (e1 <= 0)
            return vAnode * ElectricalNetwork.G_MIN;

        var anodeCurrent = Math.pow(e1, ex) / kg1;
        if (saturationCurrent > 0) {
            var saturationRatio = anodeCurrent / saturationCurrent;
            anodeCurrent /= Math.sqrt(Math.sqrt(1 + saturationRatio * saturationRatio));
        }
        return anodeCurrent;
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
        residual.add(node1.getIndex(),  linearizedAnodeCurrent);
        residual.add(node2.getIndex(), -linearizedAnodeCurrent);
    }

    @Override
    public double current() {
        return reportedCurrent;
    }

    @Override
    public double internalPower() {
        return reportedPower;
    }

    public static float calculateKg1(float anodeVoltage, float mu, float ex, float anodeCurrent) {
        var e1 = anodeVoltage / mu;
        return (float) (Math.pow(e1, ex) / anodeCurrent);
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
