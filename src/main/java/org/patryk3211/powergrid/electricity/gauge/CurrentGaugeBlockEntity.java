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
package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class CurrentGaugeBlockEntity extends GaugeBlockEntity {
    private static final float[] MAX_VALUES = new float[] { 0.02f, 0.2f, 2.0f, 20.0f };
    private ElectricWire wire;

    public CurrentGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        gaugeValue = new GaugeValueBehaviour(Component.translatable("powergrid.devices.gauge.current"),
                Unit.CURRENT.get().component(), MAX_VALUES, this, new BoxTransform());
        gaugeValue.withCallback(i -> {
            maxValue = MAX_VALUES[i];
            sendData();
        });
        behaviours.add(gaugeValue);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        maxValue = MAX_VALUES[gaugeValue.getValue()];
    }

    @Override
    public void tick() {
        var current = Math.abs(getValue());
        if(current > maxValue) {
            dialTarget = 1.125f;
        } else {
            dialTarget = current / maxValue;
        }

        applyPower(wire);
        super.tick();
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        float resistance = resistance();
        builder.setTerminalCount(2);
        wire = builder.connect(resistance, builder.terminalNode(0), builder.terminalNode(1));
    }

    @Override
    public float getValue() {
        return wire.current();
    }

    @Override
    public Unit getUnit() {
        return Unit.CURRENT;
    }

    public static ChatFormatting measurementColor(float value, float maxValue) {
        if(value < maxValue * 0.01)
            return ChatFormatting.DARK_GRAY;
        else if(value < maxValue * 0.5)
            return ChatFormatting.GREEN;
        else if(value < maxValue * 0.75)
            return ChatFormatting.YELLOW;
        else
            return ChatFormatting.RED;
    }

    public static void addTooltip(List<Component> tooltip, float current, float maxValue, boolean useMillis) {
        Lang.builder().translate("gui.current_meter.title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        String currentText;
        if(!useMillis) {
            current = Math.round(current * 100f) / 100f;
            currentText = String.format("%.2f", current);
            if(Math.abs(current) > maxValue) {
                if(current > 0)
                    currentText = String.format("> %.2f", maxValue);
                else
                    currentText = String.format("< %.2f", -maxValue);
            }
        } else {
            current = Math.round(current * 100000f) / 100f;
            currentText = String.format("%.1f", current);
            if(Math.abs(current / 1000) > maxValue) {
                if(current > 0)
                    currentText = String.format("> %.1f", maxValue * 1000);
                else
                    currentText = String.format("< %.1f", -maxValue * 1000);
            }
            current /= 1000;
        }

        Lang.builder()
                .text(currentText)
                .add(Component.nullToEmpty(useMillis ? " m" : " "))
                .add(Unit.CURRENT.get())
                .style(measurementColor(Math.abs(current), maxValue))
                .forGoggles(tooltip, 1);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        addTooltip(tooltip, getValue(), maxValue, gaugeValue.getValue() <= 1);
        return true;
    }
}
