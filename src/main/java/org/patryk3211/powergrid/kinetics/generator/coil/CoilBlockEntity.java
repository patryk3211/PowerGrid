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
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.utility.Lang;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class CoilBlockEntity extends ElectricBlockEntity implements ICoilEntity {
    protected ScrollOptionBehaviour<AggregateType> aggregateType;
    private CoilAggregate aggregate;

    private VoltageSourceNode sourceNode;
    private ICouplingNode coupling;
    private FloatingNode positive;
    private FloatingNode negative;

    private CoilBehaviour coilBehaviour;

    public CoilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        rebuildAggregate();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if(getCachedState().get(CoilBlock.HAS_TERMINALS))
            super.addBehaviours(behaviours);
        coilBehaviour = new CoilBehaviour(this);
        behaviours.add(coilBehaviour);
        aggregateType = new ScrollOptionBehaviour<>(AggregateType.class, Lang.translateDirect("devices.coil.aggregate_type"), this, new CoilValueBoxTransform());
        aggregateType.withCallback(i -> aggregate.setType(aggregateType.get()));
        aggregateType.withClientCallback(i -> aggregate.setType(aggregateType.get()));
        behaviours.add(aggregateType);
    }

    public CoilBehaviour getCoilBehaviour() {
        return coilBehaviour;
    }

    public CoilAggregate getAggregate() {
        return aggregate;
    }

    public void setAggregate(CoilAggregate aggregate) {
        this.aggregate = aggregate;
    }

    @Override
    public void initializeNodes() {
        sourceNode = new VoltageSourceNode();
        positive = new FloatingNode();
        negative = new FloatingNode();
        coupling = TransformerCoupling.create(1, CoilBlock.resistance(), sourceNode, positive, negative);
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
        if(aggregate == null)
            return 0;
        return aggregate.coilCurrent();
    }

    public float sourceNodeCurrent() {
        return sourceNode.getCurrent();
    }

    @Override
    public void tick() {
        super.tick();
        if(sourceNode != null && aggregate != null) {
            sourceNode.setVoltage(aggregate.totalVoltage());
        }
    }

    public void propagateType(AggregateType type) {
        this.aggregateType.setValue(type.ordinal());
    }

    private List<CoilBlockEntity> getNeighbors() {
        assert world != null;
        List<CoilBlockEntity> coils = new LinkedList<>();
        var facing = getCachedState().get(CoilBlock.FACING);
        for(var dir : Direction.values()) {
            if(dir.getAxis() == facing.getAxis())
                continue;
            var neighborPos = pos.offset(dir);
            if(CoilBlock.canConnect(getCachedState(), world, neighborPos) && world.getBlockEntity(neighborPos) instanceof CoilBlockEntity coil) {
                coils.add(coil);
            }
        }
        return coils;
    }

    public void rebuildAggregate() {
        assert world != null;
        var newAggregate = new CoilAggregate();
        List<CoilBlockEntity> toProcess = new LinkedList<>();
        toProcess.add(this);
        while(!toProcess.isEmpty()) {
            var coil = toProcess.remove(0);
            if(newAggregate.addCoil(coil)) {
                toProcess.addAll(coil.getNeighbors());
            }
        }
        newAggregate.apply();
    }

    public void addElectricBehaviour() {
        if(electricBehaviour == null) {
            electricBehaviour = new ElectricBehaviour(this);
            attachBehaviourLate(electricBehaviour);
            notifyUpdate();
        }
    }

    public void removeElectricBehaviour() {
        if(electricBehaviour != null) {
            electricBehaviour.breakConnections();
            electricBehaviour = null;
            removeBehaviour(ElectricBehaviour.TYPE);
            notifyUpdate();
        }
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        if(clientPacket) {
            if(tag.contains("HasOutput")) {
                if(tag.getBoolean("HasOutput")) {
                    // Must have electric behaviour.
                    addElectricBehaviour();
                } else {
                    // Cannot have electric behaviour.
                    removeElectricBehaviour();
                }
            }
        }
        super.read(tag, clientPacket);
        if(aggregate != null && aggregate.getType() != aggregateType.get()) {
            aggregate.setType(aggregateType.get());
        }
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(clientPacket) {
            tag.putBoolean("HasOutput", electricBehaviour != null);
        }
    }

    public AggregateType getAggregateType() {
        return aggregateType.get();
    }

    public enum AggregateType implements INamedIconOptions {
        // TODO: Add icons
        SERIES(AllIcons.I_ADD),
        PARALLEL(AllIcons.I_CENTERED);

        private final AllIcons icon;
        private final String translationKey;

        AggregateType(AllIcons icon) {
            this.icon = icon;
            this.translationKey = "aggregate_type." + Lang.asId(this.name());
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }

    public static class CoilValueBoxTransform extends CenteredSideValueBoxTransform {
        // TODO: Add correct transformations.
    }
}
