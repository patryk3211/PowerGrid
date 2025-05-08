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
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.IConnectableBlock;
import org.patryk3211.powergrid.collections.ModIcons;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class CoilBlockEntity extends ElectricBlockEntity implements ICoilEntity {
    protected ScrollOptionBehaviour<AggregateType> aggregateType;
    private CoilAggregate aggregate;

    private VoltageSourceNode sourceNode;
    private TransformerCoupling coupling;

    private CoilBehaviour coilBehaviour;

    public CoilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        rebuildAggregate();
        super.initialize();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if(getCachedState().get(CoilBlock.HAS_TERMINALS)) {
            electricBehaviour = new ElectricBehaviour(this);
            behaviours.add(electricBehaviour);
        }

        thermalBehaviour = specifyThermalBehaviour();
        if(thermalBehaviour != null)
            behaviours.add(thermalBehaviour);

        coilBehaviour = new CoilBehaviour(this);
        behaviours.add(coilBehaviour);

        aggregateType = new ScrollOptionBehaviour<>(AggregateType.class, Lang.translateDirect("devices.coil.aggregate_type"), this, new CoilValueBoxTransform());
        aggregateType.withCallback(i -> aggregate.setType(aggregateType.get()));
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
        if(coupling != null) {
            coupling.setResistance(aggregate.totalResistance());
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        var positive = builder.addExternalNode();
        var negative = builder.addExternalNode();

        sourceNode = builder.addInternalNode(VoltageSourceNode.class);
        coupling = builder.couple(1, aggregate != null ? aggregate.totalResistance() : CoilBlock.resistance(), sourceNode, positive, negative);
    }

    @Override
    public float windingCurrent() {
        if(aggregate == null)
            return 0;
        return aggregate.coilCurrent();
    }

    public float sourceNodeCurrent() {
        if(sourceNode == null)
            return 0;
        return sourceNode.getCurrent();
    }

    @Override
    public void tick() {
        super.tick();

        var outputCurrent = windingCurrent();
        var powerDrop = outputCurrent * outputCurrent * CoilBlock.resistance();
        if(powerDrop > 0) {
            // Coil is acting like a source
            applyLostPower(powerDrop);
        } else {
            // Coil is acting like a sink.
        }

        if(sourceNode != null && aggregate != null) {
            sourceNode.setVoltage(aggregate.totalVoltage());
        }
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return new ThermalBehaviour(this, 2.0f, 0.1f);
    }

    public void propagateType(AggregateType type) {
        this.aggregateType.setValue(type.ordinal());
        if(coupling != null) {
            coupling.setResistance(aggregate.totalResistance());
        }
    }

    public void rebuildAggregate() {
        assert world != null;
        var newAggregate = new CoilAggregate(world);
        var posList = IConnectableBlock.gatherBlocks(world, pos);
        posList.forEach(pos -> {
            var be = world.getBlockEntity(pos);
            if(be instanceof CoilBlockEntity coil)
                newAggregate.addCoil(coil);
        });
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
            // Drop nodes
            sourceNode = null;
            coupling = null;
            notifyUpdate();
        }
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        if(clientPacket) {
            if(tag.contains("HasOutput")) {
                if(tag.getBoolean("HasOutput")) {
                    // Must have electric behaviour.
                    if(aggregate != null)
                        aggregate.makeOutput(this);
                    addElectricBehaviour();
                } else {
                    // Cannot have electric behaviour.
                    if(aggregate != null)
                        aggregate.removeOutput(this);
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
        SERIES(ModIcons.I_SERIES),
        PARALLEL(ModIcons.I_PARALLEL);

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
