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
package org.patryk3211.powergrid.kinetics.base;

import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class TunedBlockEntity extends ElectricKineticBlockEntity {
    public LerpedFloat arm;

    public TunedBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        arm = LerpedFloat.linear().chase(0, 0, LerpedFloat.Chaser.LINEAR);
        arm.setValue(1);
    }

    @Override
    public void initialize() {
        super.initialize();
        arm.forceNextSync();
        sendData();
    }

    private float getChaseSpeed() {
        return Mth.clamp(Math.abs(getSpeed()) / 60.0f * 0.05f, 0, 1);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        float speed = getSpeed();
        if(speed == 0) {
            arm.chase(arm.getValue(), 0, LerpedFloat.Chaser.LINEAR);
            arm.forceNextSync();
            return;
        }
        if(sequenceContext != null && sequenceContext.instruction() == SequencerInstructions.TURN_ANGLE) {
            var angle = sequenceContext.getEffectiveValue(getTheoreticalSpeed());
            var target = Mth.clamp((arm.getValue() + angle / 315f * Math.signum(speed)), 0, 1);
            arm.chase(target, getChaseSpeed(), LerpedFloat.Chaser.LINEAR);
        } else {
            arm.chase(speed > 0 ? 1 : 0, getChaseSpeed(), LerpedFloat.Chaser.LINEAR);
        }
        sendData();
    }

    public abstract void refreshParameters();

    @Override
    public void tick() {
        super.tick();
        arm.tickChaser();

        if(!arm.settled()) {
            if(getSpeed() == 0) {
                arm.updateChaseTarget(arm.getValue());
            }
            refreshParameters();
            setChanged();
        }
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        if(clientPacket)
            arm.forceNextSync();
        compound.put("Arm", arm.writeNBT());
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        arm.readNBT(compound.getCompound("Arm"), clientPacket);
        refreshParameters();
    }
}
