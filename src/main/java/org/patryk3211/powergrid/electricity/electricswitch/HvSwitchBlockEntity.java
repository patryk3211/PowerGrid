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
package org.patryk3211.powergrid.electricity.electricswitch;

import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;

public class HvSwitchBlockEntity extends ElectricKineticBlockEntity {
    protected LerpedFloat rod;
    private SwitchedWire wire;

    private boolean wasClosed = false;

    public HvSwitchBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        rod = LerpedFloat.linear().startWithValue(0).chase(0, 0, LerpedFloat.Chaser.LINEAR);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.simple(this, 0.7f, 0.2f);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        float speed = getSpeed();
        var facing = getBlockState().getValue(HvSwitchBlock.HORIZONTAL_FACING);
        if(facing == Direction.NORTH || facing == Direction.EAST)
            speed = -speed;
        rod.chase(speed > 0 ? 1 : 0, getChaseSpeed(), LerpedFloat.Chaser.LINEAR);
        sendData();
    }

    private float getChaseSpeed() {
        return Mth.clamp(Math.abs(getSpeed()) / 21 / 20, 0, 1);
    }

    @Override
    public void initialize() {
        super.initialize();
        wire.setState(false);
        wire.setResistance(getResistance());
        wire.setState(isClosed());
    }

    @Override
    public void tick() {
        applyLostPower(wire.power());

        super.tick();
        rod.tickChaser();

        if(!rod.settled()) {
            // Setting switch to false is needed to prevent imprecision
            // messing with the conductance matrix.
            wire.setState(false);
            wire.setResistance(getResistance());
            wire.setState(isClosed());
        }
    }

    @Override
    public void tickAudio() {
        super.tickAudio();
        var closed = rod.getValue() == 1;
        var open = rod.getValue() == 0;
        if(wasClosed && open) {
            wasClosed = false;
            level.playLocalSound(getBlockPos(), ModdedSoundEvents.HV_SWITCH_DISCONNECT.getMainEvent(),
                    SoundSource.BLOCKS, 1, 1, true);
        } else if(!wasClosed && closed) {
            wasClosed = true;
            level.playLocalSound(getBlockPos(), ModdedSoundEvents.HV_SWITCH_CONNECT.getMainEvent(),
                    SoundSource.BLOCKS, 1, 1, true);
        }
    }

    public boolean isClosed() {
        if(rod == null)
            return false;
        return rod.getValue() > 0.9f;
    }

    public float getResistance() {
        if(rod == null || !isClosed())
            return resistance();
        var x = rod.getValue();
        return -999 * x + 999 + resistance();
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("Rod", rod.writeNBT());
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        tag.put("Rod", rod.writeNBT());
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        rod.readNBT(compound.getCompound("Rod"), clientPacket);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connectSwitch(getResistance(), builder.terminalNode(0), builder.terminalNode(1), isClosed());
    }
}
