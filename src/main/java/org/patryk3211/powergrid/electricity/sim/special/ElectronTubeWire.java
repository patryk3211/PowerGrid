/*
 * Copyright 2025 patryk3211
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

    private final float gain;
    private final float perveance;
    private float saturationCurrent;

    private double prevGrid;
    private double prevCathode;
    private double prevAnode;

    private double Ia;
    private float Itube;
    private float Ptube;

    private final ConductanceWire gridCathode;
    private final GMStamp gmStamp;

    public ElectronTubeWire(float gain, float perveance, float saturationCurrent, IElectricNode cathode, IElectricNode anode, IElectricNode grid) {
        super(cathode, anode);
        this.grid = grid;
        gridCathode = addDynamicWire(grid, cathode);
        gmStamp = addInternalWire(new GMStamp(cathode, anode, grid));

        this.gain = gain;
        this.perveance = perveance;
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
    public void startIteration() {
        // Implementation adapted from Falstad https://www.falstad.com/circuit-java/
        double vCathode = node1.getVoltage();
        double vGrid = grid.getVoltage();
        double vAnode = node2.getVoltage();
        var dVc = vCathode - prevCathode;
        var dVg = vGrid - prevGrid;
        var dVa = vAnode - prevAnode;
        vCathode = prevCathode + Math.log1p(Math.abs(dVc)) * 0.9 * Math.signum(dVc);
        vGrid = prevGrid + Math.log1p(Math.abs(dVg)) * 0.9 * Math.signum(dVg);
        vAnode = prevAnode + Math.log1p(Math.abs(dVa)) * 0.9 * Math.signum(dVa);
        prevAnode = vAnode;
        prevCathode = vCathode;
        prevGrid = vGrid;
        vGrid -= vCathode;
        vAnode -= vCathode;

        double ids, Gds, gm = 0;
        double ival = (vGrid * vAnode) / (vGrid + vAnode) + vAnode / gain;
        gridCathode.setConductance(GRID_CONDUCTANCE);

        if (ival < 0 || vGrid + vAnode <= 0 || saturationCurrent == 0) {
            Gds = ElectricalNetwork.G_MIN;
            ids = vAnode * Gds;
        } else {
            ids = Math.sqrt(ival * ival * ival) * perveance;
            double q = 1.5 * Math.sqrt(ival) * perveance;

            var x = ids / saturationCurrent;
            ids = ids / Math.sqrt(Math.sqrt(1 + x * x));

            Gds = q;
            gm = q / gain;
        }

        Ia = -ids + Gds * vAnode + gm * vGrid;

        var iGrid = GRID_CONDUCTANCE * vGrid;
        Itube = (float) (ids + iGrid);
        Ptube = (float) (ids * vAnode + iGrid * vGrid);

        // Anode-Cathode "wire"
        setConductance(Gds);
        gmStamp.setConductance(gm);
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        residual.add(node1.getIndex(),  Ia);
        residual.add(node2.getIndex(), -Ia);
    }

    @Override
    public float current() {
        return Itube;
    }

    @Override
    public float power() {
        return Ptube;
    }

    public static float calculatePerveance(float anodeVoltage, float gain, float anodeCurrent) {
        return (float) (anodeCurrent / Math.pow(anodeVoltage / gain, 3 / 2f));
    }

    @Override
    public String toString() {
        return String.format("ElectronTube(mu=%g G=%g Is=%g)", gain, perveance, saturationCurrent);
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
