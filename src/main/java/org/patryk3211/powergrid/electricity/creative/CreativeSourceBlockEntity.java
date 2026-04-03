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
package org.patryk3211.powergrid.electricity.creative;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.CurrentSourceNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class CreativeSourceBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    private ScrollValueBehaviour value;

    private CurrentSourceNode currentSourceNode;
    private VoltageSourceCoupling voltageSourceNode;

    private boolean overwrite = false;
    private boolean voltageSource;

    public CreativeSourceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        Component label = null;
        final float multiplier;
        if(getBlockState().is(ModdedBlocks.CREATIVE_VOLTAGE_SOURCE.get())) {
            label = Lang.translateDirect("devices.creative.voltage");
            multiplier = 1.0f;
        } else if(getBlockState().is(ModdedBlocks.CREATIVE_CURRENT_SOURCE.get())) {
            label = Lang.translateDirect("devices.creative.current");
            multiplier = 0.1f;
        } else {
            multiplier = 0.0f;
        }

        value = new CreativeSourceValueBehaviour(label, this, multiplier, new CreativeSourceBoxTransform());
        value.withCallback(i -> {
            if (!overwrite)
                setValue(((i >> 2) * ((i & 1) > 0 ? 1 : (i & 2) > 0 ? 0.001f : 1000)) * multiplier);
        });
        behaviours.add(value);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        var positive = builder.terminalNode(0);
        var negative = builder.terminalNode(1);

        if(getBlockState().is(ModdedBlocks.CREATIVE_VOLTAGE_SOURCE.get())) {
            voltageSource = true;
            voltageSourceNode = builder.addInternalNode(VoltageSourceCoupling.class, positive, negative, 1e-4f);
        } else if(getBlockState().is(ModdedBlocks.CREATIVE_CURRENT_SOURCE.get())) {
            voltageSource = false;
            currentSourceNode = builder.addInternalNode(CurrentSourceNode.class);
            // Transformer needs some resistance for solver to work correctly with the current source.
            builder.couple(1, 1e-4f, currentSourceNode, positive, negative);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if(tag.contains("Overwrite"))
            overwrite = tag.getBoolean("Overwrite");
        setValue(tag.getFloat("NodeValue"));
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(overwrite)
            tag.putBoolean("Overwrite", true);
        tag.putFloat("NodeValue", getValue());
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        if(overwrite)
            tag.putBoolean("Overwrite", true);
        tag.putFloat("NodeValue", getValue());
    }

    public void setValue(float value) {
        if(voltageSource) {
            voltageSourceNode.setVoltage(value);
        } else {
            currentSourceNode.setCurrent(value);
        }
    }

    public float getValue() {
        if(voltageSource) {
            return (float) voltageSourceNode.getVoltage();
        } else {
            return (float) currentSourceNode.getCurrent();
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Lang.translate("gui.creative_source.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.creative_source.voltage")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var voltage = (voltageSource ? voltageSourceNode.getVoltage() : currentSourceNode.getVoltage());
        var voltageText = String.format("%.2f", voltage);
        Lang.builder()
                .text(voltageText)
                .add(Component.nullToEmpty(" "))
                .add(Unit.VOLTAGE.get())
                .style(ChatFormatting.BLUE)
                .forGoggles(tooltip, 1);

        Lang.builder().translate("gui.creative_source.current")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var current = (voltageSource ? -voltageSourceNode.getCurrent() : currentSourceNode.getCurrent());
        var currentText = String.format("%.2f", current);
        Lang.builder()
                .text(currentText)
                .add(Component.nullToEmpty(" "))
                .add(Unit.CURRENT.get())
                .style(ChatFormatting.GREEN)
                .forGoggles(tooltip, 1);

        return true;
    }

    public static class CreativeSourceBoxTransform extends CenteredSideValueBoxTransform {
        public CreativeSourceBoxTransform() {
            super((state, dir) -> dir.getAxis() != Direction.Axis.Y);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0f, 8.0f, 14.5f);
        }
    }
}
