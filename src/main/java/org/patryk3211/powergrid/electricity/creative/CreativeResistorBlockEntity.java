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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.resistor.ResistorBlockEntity;
import org.patryk3211.powergrid.electricity.resistor.ResistorBoxTransform;
import org.patryk3211.powergrid.electricity.resistor.ResistorValueBehaviour;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.NumberFormats;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class CreativeResistorBlockEntity extends ResistorBlockEntity implements IHaveGoggleInformation {
    public CreativeResistorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected ResistorValueBehaviour makeScroll() {
        return new ResistorValueBehaviour(Lang.translateDirect("devices.resistor.resistance"),
                this, new ResistorBoxTransform(), 3, 72);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return null;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Lang.translate("gui.creative_resistor.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.creative_resistor.resistance")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var resistance = wire.getResistance();
        var resistanceText = NumberFormats.formatPrecise(resistance);
        Lang.builder()
                .text(resistanceText)
                .add(Component.nullToEmpty(" "))
                .add(Unit.RESISTANCE.get())
                .style(ChatFormatting.BLUE)
                .forGoggles(tooltip, 1);

        Lang.builder().translate("gui.creative_resistor.current")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        float current = (float) wire.current();
        var currentText = NumberFormats.formatPrecise(current);
        Lang.builder()
                .text(currentText)
                .add(Component.nullToEmpty(" "))
                .add(Unit.CURRENT.get())
                .style(ChatFormatting.GREEN)
                .forGoggles(tooltip, 1);

        Lang.builder().translate("gui.creative_resistor.power")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var power = NumberFormats.formatPrecise(current * current * resistance);
        Lang.builder()
                .text(power)
                .add(Component.nullToEmpty(" "))
                .add(Unit.POWER.get())
                .style(ChatFormatting.YELLOW)
                .forGoggles(tooltip, 1);
        return true;
    }

}
