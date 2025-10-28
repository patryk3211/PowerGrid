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

import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

public abstract class DynamicConductanceWire extends AbstractElectricWire implements ISolverHook {
    protected double currentConductance;
    protected double prevConductance;

    public DynamicConductanceWire(IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
        currentConductance = 0;
        prevConductance = 0;
    }

    protected abstract double calculateConductance();

    @Override
    public double conductance() {
        return currentConductance;
    }

    @Override
    public void startIteration() {
        currentConductance = calculateConductance();
        network.updateConductance(this, currentConductance - prevConductance);
        prevConductance = currentConductance;
    }
}
