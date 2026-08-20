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

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class SwitchBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    private SwitchedWire wire;
    private float maxVoltage;
    private boolean switchState;
    private Float overvoltResistance;
    private boolean isButton;
    private boolean isNormallyClosed;
    private int buttonTimeout = 0;
    private boolean playEffect = false;

    public SwitchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        isButton = ((SwitchBlock) state.getBlock()).isButton;
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    private void overvoltEffect() {
        var pos = worldPosition.getCenter();
        var face = getBlockState().getValue(SurfaceSwitchBlock.FACING);
        SparkParticleData.explodeParticles(level, (float) pos.x, (float) pos.y, (float) pos.z, face.getOpposite(), 7);
        ModdedSoundEvents.COMPONENT_EXPLODE.playAt(level, pos, 1, 1, true);
    }

    @Override
    public void electricalTick() {
        applyPower(wire);
        if(wire.isConverged() && Math.abs(wire.potentialDifference()) > maxVoltage && overvoltResistance == null) {
            wire.setState(true);
            // Pick a random resistance for failed switches to spice things up.
            overvoltResistance = level.random.nextFloat() * 1000f;
            wire.setResistance(overvoltResistance);
            playEffect = true;
            notifyUpdate();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if(isButton && buttonTimeout > 0) {
            --buttonTimeout;
            if(buttonTimeout == 0) {
                var block = (SwitchBlock) getBlockState().getBlock();
                level.setBlockAndUpdate(worldPosition, getBlockState().setValue(SwitchBlock.OPEN, true));
                block.useSound(level, worldPosition, true);
                setState(false);
            }
        }
    }

    public void setState(boolean state) {
        switchState = state;
        if(overvoltResistance == null)
            wire.setState(state != isNormallyClosed);
        if(isButton && state)
            buttonTimeout = 10;
        if(!level.isClientSide)
            notifyUpdate();
    }

    public void setNormallyClosed(boolean normallyClosed) {
        isNormallyClosed = normallyClosed;
    }

    public boolean isNormallyClosed() {
        return isNormallyClosed;
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if(isButton) {
            buttonTimeout = tag.getByte("Timeout");
            isNormallyClosed = tag.getBoolean("NormallyClosed");
            wire.setState(switchState != isNormallyClosed);
        }
        if(tag.contains("Overvolted")) {
            overvoltResistance = tag.getFloat("Overvolted");
            if(overvoltResistance <= 0)
                overvoltResistance = 1f;
            wire.setResistance(overvoltResistance);
            wire.setState(true);
            if(tag.getBoolean("Effect"))
                overvoltEffect();
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(overvoltResistance != null) {
            tag.putFloat("Overvolted", overvoltResistance);
            if(playEffect) {
                tag.putBoolean("Effect", true);
                playEffect = false;
            }
        }
        if(isButton) {
            tag.putByte("Timeout", (byte) buttonTimeout);
            tag.putBoolean("NormallyClosed", isNormallyClosed);
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        if(!(getBlockState().getBlock() instanceof SwitchBlock block))
            throw new IllegalArgumentException("Blocks with SwitchBlockEntity must inherit from SwitchBlock");
        maxVoltage = block.getMaxVoltage();
        switchState = !getBlockState().getValue(SwitchBlock.OPEN);
        wire = builder.connectSwitch(resistance(), builder.terminalNode(0), builder.terminalNode(1), switchState);
        if(overvoltResistance != null) {
            wire.setResistance(overvoltResistance);
            wire.setState(true);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(overvoltResistance == null)
            return false;
        Lang.translate("gui.damage_header")
                .forGoggles(tooltip);
        Lang.translate("gui.switch.overvolted")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        return true;
    }
}
