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
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class Voltage {
    public static void voltage(String key, ChatFormatting color, float value, Player player, List<Component> tooltip) {
        boolean hasGoggles = GogglesItem.isWearingGoggles(player);

        Lang.translate(key)
                .style(ChatFormatting.GRAY).addTo(tooltip);
        Lang.builder()
                .add(Component.nullToEmpty(" ")).add(Lang.number(value))
                .add(Component.nullToEmpty(" ")).add(Unit.VOLTAGE.get())
                .style(color).addTo(tooltip);
    }

    public static void max(float value, Player player, List<Component> tooltip) {
        voltage("tooltip.voltage.max", ChatFormatting.RED, value, player, tooltip);
    }

    public static void min(float value, Player player, List<Component> tooltip) {
        voltage("tooltip.voltage.min", ChatFormatting.DARK_AQUA, value, player, tooltip);
    }

    public static void rated(float value, Player player, List<Component> tooltip) {
        voltage("tooltip.voltage.rated", ChatFormatting.DARK_AQUA, value, player, tooltip);
    }

    public static void rpm(float value, Player player, List<Component> tooltip) {
        Lang.translate("tooltip.voltage.rpm")
                .style(ChatFormatting.GRAY).addTo(tooltip);
        Lang.builder()
                .add(Component.nullToEmpty(" ")).add(Lang.number(value))
//                .add(Text.of(" RPM/V"))
                .style(ChatFormatting.DARK_AQUA).addTo(tooltip);
    }

    public static void voc(float value, Player player, List<Component> tooltip) {
        voltage("tooltip.solar.voc", ChatFormatting.GRAY, value, player, tooltip);
    }

    public static void vmp(float value, Player player, List<Component> tooltip) {
        voltage("tooltip.solar.vmp", ChatFormatting.GRAY, value, player, tooltip);
    }
}
