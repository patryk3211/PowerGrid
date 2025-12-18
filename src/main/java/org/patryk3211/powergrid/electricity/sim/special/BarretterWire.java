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

import net.minecraft.util.Mth;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.G_MIN;

public class BarretterWire extends AbstractElectricWire implements IOuterHook {
    private final float holdingCurrent;
    private final float maximumConductance;

    private double currentConductance;

    public BarretterWire(float holdingCurrent, float minimumResistance, IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
        this.holdingCurrent = holdingCurrent;
        this.maximumConductance = 1 / minimumResistance;
        currentConductance = maximumConductance;
    }

    @Override
    public void preSolve() {
        double G_target = maximumConductance;
        if(potentialDifference() != 0) {
            G_target = holdingCurrent / Math.abs(potentialDifference());
        }
        var G = Mth.clamp(G_target, G_MIN, maximumConductance);
        network.updateConductance(this, G - currentConductance);
        currentConductance = G;
    }

    @Override
    public double conductance() {
        return currentConductance;
    }
}
