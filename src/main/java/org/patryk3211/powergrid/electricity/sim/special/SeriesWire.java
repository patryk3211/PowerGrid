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

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SeriesWire extends AbstractElectricWire implements IOuterHook {
    public final List<ElectricWire> wires = new ArrayList<>();
    public final Set<IElectricNode> nodes = new ReferenceOpenHashSet<>();
    private double conductance;

    public SeriesWire(IElectricNode node1, IElectricNode node2, List<ElectricWire> wires) {
        super(node1, node2);
        this.wires.addAll(wires);
        conductance = calculateConductance();
    }

    private double calculateConductance() {
        double R = 0;
        for(ElectricWire wire : wires)
            R += 1 / wire.conductance();
        return 1 / R;
    }

    @Override
    public void preSolve() {
        double G = calculateConductance();
        network.updateConductance(this, G - conductance);
        conductance = G;
    }

    @Override
    public void prepare(int multiTicks) {
        super.prepare(multiTicks);
        for(var wire : wires) {
            wire.prepare(multiTicks);
        }
    }

    @Override
    public void postMicroTick() {
        super.postMicroTick();
        for(var wire : wires) {
            wire.postMicroTick();
        }
    }

    @Override
    public void postUpperSolve() {
        var previousNode = node1;
        var I = current();
        double prevVoltage = previousNode.getVoltage();
        for(ElectricWire wire : wires) {
            assert wire.getNode1() == previousNode || wire.getNode2() == previousNode : "Invalid series wire formed";
            IElectricNode nextNode;
            if (wire.getNode1() == previousNode) {
                nextNode = wire.getNode2();
            } else {
                nextNode = wire.getNode1();
            }
            double voltage = prevVoltage - I / wire.conductance();
            nextNode.setSavedValue(voltage);
            previousNode = nextNode;
            prevVoltage = voltage;
        }
//        assert previousNode == node2;
    }

    @Override
    public double conductance() {
        return conductance;
    }
}
