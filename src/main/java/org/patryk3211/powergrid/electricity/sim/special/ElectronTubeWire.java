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

import org.apache.logging.log4j.util.TriConsumer;
import org.ejml.data.DMatrixRMaj;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

public class ElectronTubeWire extends CompoundWire implements ISolverHook {
    private static final double GRID_CONDUCTANCE = 1.0 / 6000.0;

    private final IElectricNode grid;

    private final float gain;
    private final float perveance;
    private float saturationCurrent;

    private double prevGrid;
    private double prevCathode;

    private double Ia;
    private float Itube;
    private float Ptube;

//    private final CapacitorWire anodeCathodeCap;
    private final ConductanceWire gridCathode;
    private final GMStamp gmStamp;

    public ElectronTubeWire(float gain, float perveance, float saturationCurrent, IElectricNode cathode, IElectricNode anode, IElectricNode grid) {
        super(cathode, anode);
        this.grid = grid;
        gridCathode = addDynamicWire(grid, cathode);
        gmStamp = addInternalWire(new GMStamp(cathode, anode, grid));
//        addInternalCapacitor(1e-9, anode, grid);
//        addInternalCapacitor(1e-8, grid, cathode);
//        anodeCathodeCap = addInternalCapacitor(1e-8, anode, cathode);

//        addInternalCapacitor(1e-9, anode, null);
//        addInternalCapacitor(1e-9, cathode, null);

        this.gain = gain;
        this.perveance = perveance;
        this.saturationCurrent = saturationCurrent;
    }

    public void setSaturationCurrent(float saturationCurrent) {
        this.saturationCurrent = saturationCurrent;
    }

    @Override
    public void preSolve() {
        // Implementation adapted from Falstad https://www.falstad.com/circuit-java/
        final var limit = 0.5f;
        double vCathode = node1.getVoltage();
        double vGrid = grid.getVoltage();
        double vAnode = node2.getVoltage();
        if(vCathode > prevCathode + limit)
            vCathode = prevCathode + limit;
        if(vCathode < prevCathode - limit)
            vCathode = prevCathode - limit;
        if(vGrid > prevGrid + limit)
            vGrid = prevGrid + limit;
        if(vGrid < prevGrid - limit)
            vGrid = prevGrid - limit;
        prevCathode = vCathode;
        prevGrid = vGrid;
        vGrid -= vCathode;
        vAnode -= vCathode;

        double ids, Gds, gm = 0;
        double ival = vGrid + vAnode / gain;
        gridCathode.setConductance(vGrid > 0.01 ? GRID_CONDUCTANCE : 0);

        if (ival < 0) {
            Gds = ElectricalNetwork.G_MIN;
            ids = vAnode * Gds;
        } else {
            ids = Math.sqrt(ival * ival * ival) * perveance;
            if(ids > saturationCurrent)
                ids = saturationCurrent;
            double q = 1.5 * Math.sqrt(ival) * perveance;
            Gds = q;
            gm = q / gain;
        }

        Ia = -ids + Gds * vAnode + gm * vGrid;

        var iGrid = vGrid > 0.01 ? GRID_CONDUCTANCE * vGrid : 0;
        Itube = (float) (ids + iGrid);
        Ptube = (float) (ids * vAnode + iGrid * vGrid);

        // Anode-Cathode "wire"
        setConductance(Gds);
        gmStamp.setConductance(gm);
    }

    @Override
    public void addResidual(DMatrixRMaj residual) {
        residual.add(node1.getIndex(), 0,  Ia);
        residual.add(node2.getIndex(), 0, -Ia);
    }

    @Override
    public float current() {
        return Itube;// + anodeCathodeCap.current();
    }

    @Override
    public float power() {
        return Ptube;
    }

    public static float calculatePerveance(float anodeVoltage, float gain, float anodeCurrent) {
        return (float) (anodeCurrent / Math.pow(anodeVoltage / gain, 3 / 2f));
    }

    private static class GMStamp extends ConductanceWire {
        private final IElectricNode node3;

        public GMStamp(IElectricNode node1, IElectricNode node2, IElectricNode node3) {
            super(node1, node2);
            this.node3 = node3;
        }

        @Override
        public void stamp(TriConsumer<Integer, Integer, Double> matrixAdd, double change) {
            // Anode-Cathode
            matrixAdd.accept(node2.getIndex(), node1.getIndex(), -change);
            // Anode-Grid
            matrixAdd.accept(node2.getIndex(), node3.getIndex(), change);
            // Cathode-Cathode
            matrixAdd.accept(node1.getIndex(), node1.getIndex(), change);
            // Cathode-Grid
            matrixAdd.accept(node1.getIndex(), node3.getIndex(), -change);
        }
    }
}
