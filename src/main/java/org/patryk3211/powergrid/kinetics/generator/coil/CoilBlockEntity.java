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
package org.patryk3211.powergrid.kinetics.generator.coil;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.*;

import java.util.Collection;
import java.util.List;

public class CoilBlockEntity extends ElectricBlockEntity implements ICoilEntity {
    private VoltageSourceNode sourceNode;
    private ICouplingNode coupling;
    private FloatingNode positive;
    private FloatingNode negative;

    private CoilBehaviour coilBehaviour;

    public CoilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        coilBehaviour = new CoilBehaviour(this);
        behaviours.add(coilBehaviour);
    }

    @Override
    public void initializeNodes() {
        sourceNode = new VoltageSourceNode();
        positive = new FloatingNode();
        negative = new FloatingNode();
        coupling = TransformerCoupling.create(1, 0.1f, sourceNode, positive, negative);
    }

    @Override
    public void addExternalNodes(List<IElectricNode> nodes) {
        nodes.add(positive);
        nodes.add(negative);
    }

    @Override
    public void addInternalNodes(Collection<INode> nodes) {
        nodes.add(sourceNode);
        nodes.add(coupling);
    }

    @Override
    public float windingCurrent() {
        return sourceNode.getCurrent();
    }

    @Override
    public void tick() {
        super.tick();
        sourceNode.setVoltage(coilBehaviour.emfVoltage());
    }
}
