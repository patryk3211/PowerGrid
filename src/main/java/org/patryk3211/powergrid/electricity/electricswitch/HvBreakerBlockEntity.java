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

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class HvBreakerBlockEntity extends ElectricKineticBlockEntity {
    @Nullable
    private SwitchedWire wire;
    private ScrollValueBehaviour setting;

    protected LerpedFloat charge;
    protected boolean prevState, state;
    private boolean redstoneState;
    private int splitCooldown;

    public HvBreakerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        charge = LerpedFloat.linear().startWithValue(0).chase(0, 0, LerpedFloat.Chaser.LINEAR);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        setting = new HvBreakerScrollBehaviour(Lang.translateDirect("gui.hv_breaker.setting"), this, new Box());
        setting.setValue(0);
        behaviours.add(setting);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        charge.chase(1, getChaseSpeed(), LerpedFloat.Chaser.LINEAR);
        if(state)
            return;
        sendData();
    }

    private float getChaseSpeed() {
        return Mth.clamp(Math.abs(getSpeed()) / 80 / 20, 0, 1);
    }

    @Override
    public void initialize() {
        assert level != null;
        super.initialize();
        redstoneState = level.getBestNeighborSignal(worldPosition) > 0;
    }

    public void trigger() {
        assert level != null;
        var redstone = level.getBestNeighborSignal(worldPosition) > 0;
        if(redstone && !redstoneState && (charge.getValue() == 1 || state)) {
            state = !state;
            if(wire != null) {
                wire.setState(state);
            } else {
                electricBehaviour.rebuildCircuit(false);
            }
            charge.setValueNoUpdate(state ? 1 : 0);
            (state ? ModdedSoundEvents.BREAKER_ON : ModdedSoundEvents.BREAKER_OFF)
                    .playOnServer(level, worldPosition);
            notifyUpdate();
        }
        redstoneState = redstone;
    }

    @Override
    public void tick() {
        assert level != null;
        applyPower(wire);

        super.tick();
        prevState = state;
        charge.tickChaser();

        if(getChaseSpeed() != 0 && !charge.settled()) {
            int soundRate = (int) (1 / (getChaseSpeed() * 10)) + 1;
            float volume = .25f;
            float pitch = charge.getValue() + 0.5f;
            if (((int) level.getGameTime()) % soundRate == 0)
                level.playSound(null, worldPosition, SoundEvents.WOODEN_BUTTON_CLICK_OFF, SoundSource.BLOCKS,
                        volume, pitch);
        } else if(charge.getValue() == 1 && charge.getValue(0) != 1) {
            level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS,
                    .25f, 1.5f);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }

        if(wire != null) {
            if (state && setting.getValue() != 0 && !level.isClientSide && wire.isConverged()) {
                if (Math.abs(wire.current()) > setting.getValue()) {
                    state = false;
                    wire.setState(false);
                    charge.setValueNoUpdate(0);
                    ModdedSoundEvents.BREAKER_OFF.playOnServer(level, worldPosition);
                    notifyUpdate();
                }
            }
            if (!state && splitCooldown++ >= 100) {
                GlobalElectricNetworks.getWorldNetworks(level).scheduleIslandDiscovery(wire.getNetwork());
                electricBehaviour.rebuildCircuit(false);
            }
        }
    }

    public int getSignal() {
        if(state)
            return 15;
        if(charge.getValue() == 1)
            return 1;
        return 0;
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("Charge", charge.writeNBT());
        compound.putBoolean("State", state);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        // Always force the value to be set
        charge.readNBT(compound.getCompound("Charge"), clientPacket);
        if(compound.contains("Charge"))
            charge.setValueNoUpdate(compound.getCompound("Charge").getFloat("Value"));
        state = compound.getBoolean("State");
        if(wire != null) {
            wire.setState(state);
        } else {
            electricBehaviour.rebuildCircuit(false);
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        if(state) {
            splitCooldown = 0;
            wire = builder.connectSwitch(resistance(), builder.terminalNode(0), builder.terminalNode(1), state);
        } else {
            wire = null;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Lang.translate("gui.hv_breaker.status")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        LangBuilder line;
        if(state) {
            line = Lang.translate("gui.hv_breaker.closed")
                    .style(ChatFormatting.RED);
        } else {
            line = Lang.translate("gui.hv_breaker.open")
                    .style(ChatFormatting.GREEN);
        }
        if(charge.getValue() == 1) {
            line.add(Lang.text(" - ").style(ChatFormatting.DARK_GRAY))
                    .add(Lang.translate("gui.hv_breaker.charged").style(ChatFormatting.YELLOW));
        }
        line.forGoggles(tooltip, 1);
        return true;
    }

    public static class Box extends CenteredSideValueBoxTransform {
        public Box() {
            super((state, dir) -> dir == state.getValue(HvBreakerBlock.HORIZONTAL_FACING));
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0f, 7.0f, 15.5f);
        }
    }
}
