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

import org.ejml.data.DMatrixRMaj;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

public class ElectronTubeWire extends AbstractElectricWire implements ISolverHook {
    public static final float I_LEAK = 1e-6f;

    private final IElectricNode grid;
    private final ElectricWire gridWire;

    private final float gain;
    private final float perveance;
    private float saturationCurrent;

    private double prevConductance;

    public ElectronTubeWire(float gain, float perveance, float saturationCurrent, IElectricNode cathode, IElectricNode anode, IElectricNode grid) {
        super(cathode, anode);
        this.grid = grid;
        this.gridWire = new ElectricWire(1e5f, grid, cathode);

        this.gain = gain;
        this.perveance = perveance;
        this.saturationCurrent = saturationCurrent;

        this.prevConductance = 0;
    }

    public void setSaturationCurrent(float saturationCurrent) {
        this.saturationCurrent = saturationCurrent;
    }

    @Override
    public void setNetwork(ElectricalNetwork network) {
        super.setNetwork(network);
        if(network != null) {
            network.addWire(gridWire);
        } else {
            gridWire.remove();
            gridWire.setNetwork(null);
        }
    }

    @Override
    public void remove() {
        super.remove();
        gridWire.remove();
    }

    @Override
    public double conductance() {
        var cathodeVoltage = node1.getVoltage();
        var gridPotential = grid.getVoltage() - cathodeVoltage;
        var anodePotential = node2.getVoltage() - cathodeVoltage;

        if(anodePotential > 0 && saturationCurrent > 0) {
            // Somewhat realistic triode current equation:
            var x = gridPotential + anodePotential / gain;
            if(x <= 0)
                return I_LEAK;
            var Ia = perveance * /*Math.sqrt(x * x * x)*/ x + I_LEAK;
            Ia = Math.min(Ia, saturationCurrent);
            return Ia / anodePotential;
        } else {
            return 0;
        }
    }

    @Override
    public void preSolve() {
        var newConductance = conductance();
        network.updateConductance(this, newConductance - prevConductance);
        prevConductance = newConductance;
    }

    public static float calculatePerveance(float anodeVoltage, float gain, float anodeCurrent) {
        return (float) (anodeCurrent / Math.pow(anodeVoltage / gain, 3 / 2f));
    }
}
