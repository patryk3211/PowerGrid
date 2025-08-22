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
package org.patryk3211.powergrid.circuits.circuitboard;

import net.minecraft.core.BlockPos;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;

import java.util.Collection;
import java.util.function.Function;

public class ComponentCircuitBuilder extends IElectricEntity.CircuitBuilder {
    private final Function<Integer, FloatingNode> externalNodeProvider;

    public ComponentCircuitBuilder(BlockPos pos, Function<Integer, FloatingNode> externalNodeProvider, Collection<INode> internalNodes, Collection<AbstractElectricWire> wires) {
        super(pos, null, internalNodes, wires);
        this.externalNodeProvider = externalNodeProvider;
    }

    @Override
    protected void addExternalNode() {
        throw new IllegalCallerException("Cannot add external node in ComponentCircuitBuilder");
    }

    @Override
    public void setTerminalCount(int count) {
        throw new IllegalCallerException("Cannot add external node in ComponentCircuitBuilder");
    }

    @Override
    public void setExternalNode(int index, boolean present) {
        throw new IllegalCallerException("Cannot alter external node in ComponentCircuitBuilder");
    }

    @Override
    public FloatingNode terminalNode(int index) {
        return externalNodeProvider.apply(index);
    }
}
