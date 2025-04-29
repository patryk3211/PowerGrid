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

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.utility.Lang;

import java.util.Collection;
import java.util.List;

public class CreativeSourceBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    private ScrollValueBehaviour value;

    private ElectricNode sourceNode;
    private CouplingNode coupling;
    private FloatingNode positive;
    private FloatingNode negative;

    private boolean voltageSource;

    public CreativeSourceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        Text label = null;
        final float multiplier;
        if(getCachedState().isOf(ModdedBlocks.CREATIVE_VOLTAGE_SOURCE.get())) {
            label = Lang.translateDirect("devices.creative.voltage");
            multiplier = 1.0f;
        } else if(getCachedState().isOf(ModdedBlocks.CREATIVE_CURRENT_SOURCE.get())) {
            label = Lang.translateDirect("devices.creative.current");
            multiplier = 0.1f;
        } else {
            multiplier = 0.0f;
        }

        value = new ScrollValueBehaviour(label, this, new CreativeSourceBoxTransform());
        value.between(-250, 250).withFormatter(i -> String.format("%.1f", i * multiplier));
        value.withCallback(i -> setValue(i * multiplier));
        behaviours.add(value);
    }

    @Override
    public void initializeNodes() {
        if(getCachedState().isOf(ModdedBlocks.CREATIVE_VOLTAGE_SOURCE.get())) {
            voltageSource = true;
            sourceNode = new VoltageSourceNode();
        } else if(getCachedState().isOf(ModdedBlocks.CREATIVE_CURRENT_SOURCE.get())) {
            voltageSource = false;
            sourceNode = new CurrentSourceNode();
        } else {
            throw new IllegalArgumentException();
        }
        positive = new FloatingNode();
        negative = new FloatingNode();
        coupling = TransformerCoupling.create(1, sourceNode, positive, negative);
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        setValue(tag.getFloat("NodeValue"));
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("NodeValue", getValue());
    }

    public void setValue(float value) {
        if(voltageSource) {
            sourceNode.setVoltage(value);
        } else {
            sourceNode.setCurrent(value);
        }
    }

    public float getValue() {
        if(voltageSource) {
            return sourceNode.getVoltage();
        } else {
            return sourceNode.getCurrent();
        }
    }

    @Override
    public void addInternalNodes(Collection<INode> nodes) {
        nodes.add(sourceNode);
        nodes.add(coupling);
    }

    @Override
    public void addExternalNodes(List<IElectricNode> nodes) {
        nodes.add(positive);
        nodes.add(negative);
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        Lang.translate("gui.creative_source.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.creative_source.voltage")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);

        var voltage = sourceNode.getVoltage();
        var voltageText = String.format("%.2f", voltage);
        var volt = Lang.builder().translate("generic.unit.volt");
        Lang.builder()
                .text(voltageText)
                .add(volt)
                .style(Formatting.BLUE)
                .forGoggles(tooltip, 1);

        Lang.builder().translate("gui.creative_source.current")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);

        var current = sourceNode.getCurrent();
        var currentText = String.format("%.2f", current);
        var amp = Lang.builder().translate("generic.unit.amp");
        Lang.builder()
                .text(currentText)
                .add(amp)
                .style(Formatting.GREEN)
                .forGoggles(tooltip, 1);

        return true;
    }

    public static class CreativeSourceBoxTransform extends CenteredSideValueBoxTransform {
        public CreativeSourceBoxTransform() {
            super((state, dir) -> dir.getAxis() != Direction.Axis.Y);
        }

        @Override
        protected Vec3d getSouthLocation() {
            return VecHelper.voxelSpace(8.0f, 8.0f, 14.5f);
        }
    }
}
