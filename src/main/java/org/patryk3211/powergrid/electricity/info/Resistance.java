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
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class Resistance {
    private static void resistance(LangBuilder tooltipText, float value, Player player, List<Component> tooltip, LangBuilder unit) {
        if(value == 0)
            return;
        boolean hasGoggles = GogglesItem.isWearingGoggles(player);
        if(!hasGoggles)
            return;

        tooltipText.addTo(tooltip);
        LangBuilder valueText = Lang.builder().add(Component.nullToEmpty(" "));
        if(value < 1) {
            // Millis
            valueText.add(Lang.number(value * 1000))
                    .add(Component.nullToEmpty(" m"))
                    .add(unit);
        } else if(value < 1000) {
            valueText.add(Lang.number(value))
                    .add(Component.nullToEmpty(" "))
                    .add(unit);
        } else if(value < 1000000) {
            valueText.add(Lang.number(value / 1000))
                    .add(Component.nullToEmpty(" k"))
                    .add(unit);
        } else {
            valueText.add(Lang.number(value / 1000000))
                    .add(Component.nullToEmpty(" M"))
                    .add(unit);
        }
        valueText.style(ChatFormatting.DARK_AQUA).addTo(tooltip);
    }

    private static void resistance(LangBuilder tooltipText, float value, Player player, List<Component> tooltip) {
        resistance(tooltipText, value, player, tooltip, Unit.RESISTANCE.get());
    }

    public static void series(float value, Player player, List<Component> tooltip) {
        resistance(Lang.translate("tooltip.resistance.series").style(ChatFormatting.GRAY), value, player, tooltip);
    }

    public static void seriesPerMeter(float value, Player player, List<Component> tooltip) {
        resistance(Lang.translate("tooltip.resistance.series").style(ChatFormatting.GRAY), value, player, tooltip, Lang.unit("ohm_meter"));
    }

    public static void minimum(float value, Player player, List<Component> tooltip) {
        resistance(Lang.translate("tooltip.resistance.minimum").style(ChatFormatting.GRAY), value, player, tooltip);
    }

    public static void maximum(float value, Player player, List<Component> tooltip) {
        resistance(Lang.translate("tooltip.resistance.maximum").style(ChatFormatting.GRAY), value, player, tooltip);
    }

    public static void switchResistance(float value, Player player, List<Component> tooltip) {
        resistance(Lang.translate("tooltip.resistance.switch").style(ChatFormatting.GRAY), value, player, tooltip);
    }

    public static void coil(float value, Player player, List<Component> tooltip) {
        resistance(Lang.translate("tooltip.resistance.coil").style(ChatFormatting.GRAY), value, player, tooltip);
    }
}
