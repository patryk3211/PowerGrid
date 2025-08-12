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
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PreciseNumberFormat;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class CreativeResistorBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    private ScrollValueBehaviour value;

    private ElectricWire wire;

    public CreativeResistorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        value = new CreativeResistorValueBehaviour(Lang.translateDirect("devices.creative.resistance"), this, new BoxTransform());
        value.value = 45;
        value.withCallback(i -> wire.setResistance(CreativeResistorValueBehaviour.exponentialValue(i)));
        behaviours.add(value);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        var terminal1 = builder.addExternalNode();
        var terminal2 = builder.addExternalNode();
        wire = builder.connect(100f, terminal1, terminal2);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        wire.setResistance(tag.getFloat("Resistance"));
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("Resistance", (float) wire.getResistance());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Lang.translate("gui.creative_resistor.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.creative_resistor.resistance")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var resistance = wire.getResistance();
        var resistanceText = PreciseNumberFormat.format(resistance);
        Lang.builder()
                .text(resistanceText)
                .add(Component.nullToEmpty(" "))
                .add(Unit.RESISTANCE.get())
                .style(ChatFormatting.BLUE)
                .forGoggles(tooltip, 1);

        Lang.builder().translate("gui.creative_resistor.current")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        float current = wire.current();
        var currentText = PreciseNumberFormat.format(current);
        Lang.builder()
                .text(currentText)
                .add(Component.nullToEmpty(" "))
                .add(Unit.CURRENT.get())
                .style(ChatFormatting.GREEN)
                .forGoggles(tooltip, 1);

        Lang.builder().translate("gui.creative_resistor.power")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var power = PreciseNumberFormat.format(current * current * resistance);
        Lang.builder()
                .text(power)
                .add(Component.nullToEmpty(" "))
                .add(Unit.POWER.get())
                .style(ChatFormatting.YELLOW)
                .forGoggles(tooltip, 1);
        return true;
    }

    public static class BoxTransform extends CenteredSideValueBoxTransform {
        public BoxTransform() {
            super((state, dir) -> {
                if(dir.getAxis() == state.getValue(CreativeResistorBlock.HORIZONTAL_AXIS))
                    return false;
                return dir != Direction.DOWN;
            });
        }

        @Override
        protected Vec3 getSouthLocation() {
            if(direction != Direction.UP)
                return VecHelper.voxelSpace(8.0f, 6.0f, 10.5f);
            else
                return VecHelper.voxelSpace(8.0f, 8.0f, 8.5f);
        }
    }
}
