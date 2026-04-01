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
package org.patryk3211.powergrid.electricity.info;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class Current {
    public static void max(float value, Player player, List<Component> tooltip) {
        boolean hasGoggles = GogglesItem.isWearingGoggles(player);

        Lang.translate("tooltip.current.max")
                .style(ChatFormatting.GRAY).addTo(tooltip);
        Lang.builder()
                .add(Component.nullToEmpty(" ")).add(Lang.number(value))
                .add(Component.nullToEmpty(" ")).add(Unit.CURRENT.get())
                .style(ChatFormatting.RED).addTo(tooltip);
    }

    public static void min(float value, Player player, List<Component> tooltip) {

        Lang.translate("tooltip.current.min")
                .style(ChatFormatting.GRAY).addTo(tooltip);
        Lang.builder()
                .add(Component.nullToEmpty(" ")).add(Lang.number(value))
                .add(Component.nullToEmpty(" ")).add(Unit.CURRENT.get())
                .style(ChatFormatting.RED).addTo(tooltip);
    }


    public static void max(ItemStack stack, Player player, List<Component> tooltip) {
        if(stack.getItem() instanceof BlockItem blockItem) {
            var block = blockItem.getBlock();
            var resistance = ResistanceValues.get(block);
            var power = ThermalValues.getPower(block);
            var current = Math.sqrt(power / resistance);
            max((float) Math.round(current * 10) / 10, player, tooltip);
        }
    }

    public static void max(float resistance, float power, Player player, List<Component> tooltip) {
        var current = Math.sqrt(power / resistance);
        max((float) Math.round(current * 10) / 10, player, tooltip);
    }
}
