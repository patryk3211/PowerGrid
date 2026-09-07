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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchBlock;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;

import java.util.List;

public class PunchCardReaderBlockEntity extends ElectricKineticBlockEntity {
    private SwitchedWire[] wires;

    protected float prevAngle;
    protected float angle;
    private Float maxAngle;
    private int oldIndex = -1;

    public PunchCardReaderBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
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
    public @Nullable AThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    private float getChaseSpeed() {
        float speed = getSpeed();
        var facing = getBlockState().getValue(HvSwitchBlock.HORIZONTAL_FACING);
        if(facing == Direction.NORTH || facing == Direction.EAST)
            speed = -speed;
        return speed / 256f;
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if(sequenceContext != null) {
            maxAngle = (float) (angle + Math.abs(sequenceContext.getEffectiveValue(getSpeed()) / 72f) * Math.signum(getChaseSpeed()));
        } else {
            maxAngle = null;
        }
        sendData();
    }

    public int getRedstoneOutput() {
        var item = currentItem();
        return item.isEmpty() ? 0 : Math.min(Mth.floor(angle), 15);
    }

    @Override
    public void electricalTick() {
        assert level != null;
        super.electricalTick();
        for(int i = 0; i < 8; ++i)
            applyPower(wires[i]);
        var item = currentItem();
        var index = item.isEmpty() ? -1 : Math.min(Mth.floor(angle), 15);
        if(index != oldIndex) {
            byte value = 0;
            if(!item.isEmpty() && item.has(DataComponents.CUSTOM_DATA)) {
                var data = item.get(DataComponents.CUSTOM_DATA).copyTag().getByteArray("Data");
                if(data.length == 16) {
                    value = data[index];
                }
            }
            for (int i = 0; i < 8; ++i) {
                wires[i].setState((value & (1 << i)) != 0);
            }
            level.playSound(null, worldPosition, SoundEvents.WOODEN_BUTTON_CLICK_OFF,
                    SoundSource.BLOCKS, 0.25f, 1.5f);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            oldIndex = index;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(!currentItem().isEmpty()) {
            prevAngle = angle;
            double speed = getChaseSpeed();
            angle += speed;
            if(maxAngle != null) {
                if(speed > 0 && angle > maxAngle)
                    angle = maxAngle;
                if(speed < 0 && angle < maxAngle)
                    angle = maxAngle;
            }
            if (angle > 16) angle = 16;
            if (angle < 0) angle = 0;
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        // Always sync
        prevAngle = angle = compound.getFloat("Angle");
        if(clientPacket)
            maxAngle = compound.contains("MaxAngle") ? compound.getFloat("MaxAngle") : null;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putFloat("Angle", angle);
        if(clientPacket && maxAngle != null)
            compound.putFloat("MaxAngle", maxAngle);
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

    public void dropItems() {

    }
}
