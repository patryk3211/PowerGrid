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
package org.patryk3211.powergrid.kinetics.punchcard;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchBlock;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;

import java.util.List;

public class PunchCardReaderBlockEntity extends ElectricKineticBlockEntity {
    private SwitchedWire[] wires;

    protected final LerpedFloat progress = LerpedFloat.linear();
    private int oldIndex = -1;

    public PunchCardReaderBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        progress.chase(0, 0, LerpedFloat.Chaser.LINEAR);
    }

    public ItemStack currentItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour.reducedSync();
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    private float getChaseSpeed() {
        return Mth.clamp(Math.abs(getSpeed()) / (80 * 10 * 16), 0, 1);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        float speed = getSpeed();
        var facing = getBlockState().getValue(HvSwitchBlock.HORIZONTAL_FACING);
        if(facing == Direction.NORTH || facing == Direction.EAST)
            speed = -speed;
        progress.chase(speed > 0 ? 1 : 0, getChaseSpeed(), LerpedFloat.Chaser.LINEAR);
        sendData();
    }

    @Override
    public void electricalTick() {
        assert level != null;
        super.electricalTick();
        for(int i = 0; i < 8; ++i)
            applyPower(wires[i]);
        var item = currentItem();
        var index = item.isEmpty() ? -1 : Math.min(Mth.floor(progress.getValue() * 16), 15);
        if(index != oldIndex) {
            byte value = 0;
            if(!item.isEmpty() && item.hasTag()) {
                var data = item.getTag().getByteArray("Data");
                if(data.length == 16) {
                    value = data[index];
                }
            }
            for (int i = 0; i < 8; ++i) {
                wires[i].setState((value & (1 << i)) != 0);
            }
            level.playSound(null, worldPosition, SoundEvents.WOODEN_BUTTON_CLICK_OFF,
                    SoundSource.BLOCKS, 0.25f, 1.5f);
            oldIndex = index;
        }
    }

    @Override
    public void tick() {
        progress.tickChaser();
        super.tick();
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        // Always sync
        progress.readNBT(compound.getCompound("Progress"), false);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("Progress", progress.writeNBT());
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(9);
        wires = new SwitchedWire[8];
        var common = builder.terminalNode(0);
        for(int i = 0; i < 8; ++i) {
            wires[i] = builder.connectSwitch(resistance(), common, builder.terminalNode(i + 1), false);
        }
    }

    public boolean insertCard(ItemStack stack, Direction side) {
        throw new AssertionError("Implementation dependent");
    }

    public ItemStack extractCard() {
        throw new AssertionError("Implementation dependent");
    }
}
