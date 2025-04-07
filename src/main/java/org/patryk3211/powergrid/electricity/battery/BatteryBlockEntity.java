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
package org.patryk3211.powergrid.electricity.battery;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.*;

import java.util.Collection;
import java.util.List;

public class BatteryBlockEntity extends ElectricBlockEntity {
    private IElectricNode sourceNode;
    private IElectricNode positive;
    private IElectricNode negative;
    private ICouplingNode coupling;

    public BatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initializeNodes() {
        positive = new FloatingNode();
        negative = new FloatingNode();
        sourceNode = new VoltageSourceNode(12);
        coupling = TransformerCoupling.create(1, sourceNode, positive, negative);
    }

    @Override
    public void addInternalNodes(Collection<INode> nodes) {
        nodes.add(sourceNode);
        nodes.add(coupling);
    }

    @Override
    public void addExternalNodes(List<IElectricNode> nodes) {
        nodes.add(positive);
        nodes.add(negative);
    }
}
