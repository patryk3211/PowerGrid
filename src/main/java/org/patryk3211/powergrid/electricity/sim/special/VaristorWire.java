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

import net.minecraft.util.Mth;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;

public class VaristorWire extends AbstractElectricWire implements IOuterHook {
    private final double offConductance;
    private final double thresholdVoltage;

    private double integrated;
    private double currentConductance;

    public VaristorWire(IElectricNode node1, IElectricNode node2, double offConductance, double thresholdVoltage) {
        super(node1, node2);
        this.offConductance = offConductance;
        this.thresholdVoltage = thresholdVoltage;
        this.currentConductance = offConductance;
    }

    @Override
    public double conductance() {
        return currentConductance;
    }

    @Override
    public void preSolve() {
        var x = Math.max(Math.abs(potentialDifference()) / thresholdVoltage - 0.75, 0) * 2;
        integrated = integrated * 0.5 + x * x;
        var conductance = offConductance + offConductance * 2000 * Mth.clamp(integrated, 0, 5);
        network.updateConductance(this, conductance - this.currentConductance);
        this.currentConductance = conductance;
    }
}
