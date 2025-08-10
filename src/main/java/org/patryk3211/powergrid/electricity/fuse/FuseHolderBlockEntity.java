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

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
        this.state = state.get(FuseHolderBlock.STATE);
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
        fuseWire = builder.connectSwitch(0.2f, builder.terminalNode(0), builder.terminalNode(1), state == FuseState.CLOSED);
    }

    @Environment(EnvType.CLIENT)
    public void playEffect() {
        if(world == null)
            return;
        var pos = this.pos.toCenterPos();
        var facing = getCachedState().get(FuseHolderBlock.FACING);
        SparkParticleData.explodeParticles(world, (float) pos.x, (float) pos.y, (float) pos.z, facing.getOpposite(), 5);
        ModdedSoundEvents.FUSE_POPS.playAt(world, pos, 1.0f, 1.0f, false);
    }

    @Override
    public void tick() {
        super.tick();
        if(fuseWire.getState()) {
            if(Math.abs(fuseWire.current()) > setting.value) {
                setState(FuseState.BLOWN);
                if(world.isClient)
                    playEffect();
            }
        }
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        var prevState = state;
        state = FuseState.values()[tag.getInt("State")];
        if(clientPacket && state == FuseState.BLOWN && prevState == FuseState.CLOSED)
            playEffect();
        fuseWire.setState(state == FuseState.CLOSED);
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("State", state.ordinal());
    }

    public boolean resetFuse() {
        if(state == FuseState.OPEN || state == FuseState.BLOWN) {
            if(!world.isClient) {
                setState(FuseState.CLOSED);
                ModdedSoundEvents.FUSE_INSTALL.playOnServer(world, pos);
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
            if(world != null) {
                world.setBlockState(pos, getCachedState().with(FuseHolderBlock.STATE, state));
                if(!world.isClient)
                    notifyUpdate();
            }
            fuseWire.setState(state == FuseState.CLOSED);
        }
    }

    public static class BoxTransform extends CenteredSideValueBoxTransform {
        public BoxTransform() {
            super((state, dir) -> dir.getOpposite() == state.get(FuseHolderBlock.FACING));
        }

        @Override
        protected Vec3d getSouthLocation() {
            return VecHelper.voxelSpace(8.0f, 8.0f, 9.0f);
        }
    }
}
