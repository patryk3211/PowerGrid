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
package org.patryk3211.powergrid.electricity.fuse;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class FuseHolderBlockEntity extends ElectricBlockEntity {
    private ScrollValueBehaviour setting;
    private SwitchedWire fuseWire;

    private FuseState state;

    public FuseHolderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.state = state.getValue(FuseHolderBlock.STATE);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        setting = new ScrollValueBehaviour(Lang.translateDirect("devices.fuse.setting"), this, new BoxTransform())
                .withFormatter(i -> Integer.toString(Math.max(1, i)))
                .between(1, 100);
        setting.value = 10;
        behaviours.add(setting);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        fuseWire = builder.connectSwitch(resistance(), builder.terminalNode(0), builder.terminalNode(1), state == FuseState.CLOSED);
    }

    @Environment(EnvType.CLIENT)
    public void playEffect() {
        if(level == null)
            return;
        var pos = this.worldPosition.getCenter();
        var facing = getBlockState().getValue(FuseHolderBlock.FACING);
        SparkParticleData.explodeParticles(level, (float) pos.x, (float) pos.y, (float) pos.z, facing.getOpposite(), 5);
        ModdedSoundEvents.FUSE_POPS.playAt(level, pos, 1.0f, 1.0f, false);
    }

    @Override
    public void electricalTick() {
        if(fuseWire.getState()) {
            if(fuseWire.isConverged() && Math.abs(fuseWire.current()) > setting.value) {
                setState(FuseState.BLOWN);
            }
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        var prevState = state;
        state = FuseState.values()[tag.getInt("State")];
        if(clientPacket && state == FuseState.BLOWN && prevState == FuseState.CLOSED)
            playEffect();
        fuseWire.setState(state == FuseState.CLOSED);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("State", state.ordinal());
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        tag.putInt("State", state.ordinal());
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state) {
        if(this.state == FuseState.CLOSED)
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ModdedItems.IRON_WIRE.asStack());
        return ItemRequirement.NONE;
    }

    public boolean resetFuse() {
        if(state == FuseState.OPEN || state == FuseState.BLOWN) {
            if(!level.isClientSide) {
                setState(FuseState.CLOSED);
                ModdedSoundEvents.FUSE_INSTALL.playOnServer(level, worldPosition);
            }
            return true;
        }
        return false;
    }

    public boolean removeBlown() {
        if(state == FuseState.BLOWN) {
            setState(FuseState.OPEN);
            return true;
        }
        return false;
    }

    public void setState(FuseState state) {
        if(this.state != state) {
            this.state = state;
            if(level != null) {
                level.setBlockAndUpdate(worldPosition, getBlockState().setValue(FuseHolderBlock.STATE, state));
                if(!level.isClientSide)
                    notifyUpdate();
            }
            fuseWire.setState(state == FuseState.CLOSED);
        }
    }

    public static class BoxTransform extends CenteredSideValueBoxTransform {
        public BoxTransform() {
            super((state, dir) -> dir.getOpposite() == state.getValue(FuseHolderBlock.FACING));
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0f, 8.0f, 5.0f);
        }
    }
}
