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
package org.patryk3211.powergrid.forge;

import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.utility.Lang;

import static net.minecraft.ChatFormatting.*;

public class ElectricProperties implements TooltipModifier {
    private final IHaveElectricProperties properties;

    protected ElectricProperties(IHaveElectricProperties properties) {
        this.properties = properties;
    }

    public static ElectricProperties create(Item item) {
        if(item instanceof IHaveElectricProperties properties) {
            return new ElectricProperties(properties);
        }
        if(item instanceof BlockItem blockItem) {
            if(blockItem.getBlock() instanceof IHaveElectricProperties properties) {
                return new ElectricProperties(properties);
            }
        }
        return null;
    }

    private Component header(boolean shift) {
        String[] holdDesc = Lang.translateDirect("tooltip.holdForDescription", "$")
                .getString()
                .split("\\$");
        MutableComponent keyShift = CreateLang.translateDirect("tooltip.keyShift");
        MutableComponent tabBuilder = Component.empty();
        tabBuilder.append(Component.literal(holdDesc[0]).withStyle(DARK_GRAY));
        tabBuilder.append(keyShift.plainCopy()
                .withStyle(shift ? WHITE : GRAY));
        tabBuilder.append(Component.literal(holdDesc[1]).withStyle(DARK_GRAY));
        return tabBuilder;
    }

    @Override
    public void modify(ItemTooltipEvent context) {
        var hasSummary = false;
        for(var line : context.getToolTip()) {
            var siblings = line.getSiblings();
            if(siblings.size() < 2)
                continue;
            var key = line.getSiblings().get(1);
            if(key.getContents() instanceof TranslatableContents) {
                // If the structure matches we assume that the summary thing is present.
                hasSummary = true;
                break;
            }
        }

        var shift = EnvExecutor.getInEnv(Env.CLIENT, () -> Screen::hasShiftDown).orElse(false);
        if(!hasSummary) {
            context.getToolTip().add(header(shift));
        }
        if(shift) {
            properties.appendProperties(context.getItemStack(), context.getEntity(), context.getToolTip());
        }
    }
}
