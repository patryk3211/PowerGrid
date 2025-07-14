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
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

public class CapacitorWire extends AbstractElectricWire implements ISolverHook {
    private float capacitance;
    private float voltageInject;

    public CapacitorWire(float capacitance, IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
        this.capacitance = capacitance;
        this.voltageInject = 0;
    }

    @Override
    public double conductance() {
        // dt = 50ms (1 tick)
        return capacitance / 0.05f;
    }

    public void setVoltage(float voltage) {
        voltageInject = voltage - potentialDifference();
    }

    @Override
    public float current() {
        // TODO: Current calculation needs to be handled differently for capacitors.
        return 0;
    }

    @Override
    public void addResidual(DMatrixRMaj A, DMatrixRMaj x, DMatrixRMaj b, DMatrixRMaj residual) {
        // Calculate current with a bit of leakage
        var current = conductance() * (potentialDifference() + voltageInject) * 0.9999f;
        if(node1 != null)
            residual.add(node1.getIndex(), 0, current);
        if(node2 != null)
            residual.add(node2.getIndex(), 0, -current);
        voltageInject = 0;
    }
}
