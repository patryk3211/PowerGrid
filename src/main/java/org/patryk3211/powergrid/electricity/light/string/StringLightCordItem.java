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
package org.patryk3211.powergrid.electricity.light.string;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.wire.powercord.CordItem;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class StringLightCordItem extends CordItem {
    public StringLightCordItem(Properties settings) {
        super(settings, StringLightCordEntity::create);
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        super.appendProperties(stack, player, tooltip);
        Voltage.rated(120, player, tooltip);
        Power.rated(3, player, tooltip);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, tooltipContext, tooltipComponents, isAdvanced);
        if(!stack.has(DataComponents.CUSTOM_DATA))
            return;
        var pattern = stack.get(DataComponents.CUSTOM_DATA).copyTag().getByteArray("Pattern");
        if(pattern.length == 0)
            return;
        var line = Lang.translate("tooltip.string_light_cord_pattern")
                .style(ChatFormatting.GRAY);
        for(var b : pattern) {
            var color = DyeColor.byId(b);
            line.text(color.getTextColor(), "O");
        }
        tooltipComponents.add(line.component());
    }
}
