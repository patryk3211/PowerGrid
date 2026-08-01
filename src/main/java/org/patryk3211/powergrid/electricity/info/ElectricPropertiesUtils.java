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

import com.simibubi.create.foundation.utility.CreateLang;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

import static net.minecraft.ChatFormatting.*;

public class ElectricPropertiesUtils {
    public static Component header(boolean shift) {
        MutableComponent keyShift = CreateLang.translateDirect("tooltip.keyShift")
                .plainCopy()
                .withStyle(shift ? WHITE : GRAY);
        return Lang.translateDirect("tooltip.holdForDescription", keyShift)
                .withStyle(DARK_GRAY);
    }

    public static void modify(IHaveElectricProperties properties, ItemStack stack, Player player, TooltipFlag flags, List<Component> tooltip) {
        var hasSummary = false;
        for(var line : tooltip) {
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

        if(!properties.alwaysDisplay()) {
            var shift = EnvExecutor.getInEnv(Env.CLIENT, () -> Screen::hasShiftDown).orElse(false);
            if (!hasSummary) {
                tooltip.add(header(shift));
            }
            if (shift) {
                properties.appendProperties(stack, player, tooltip);
            }
        } else {
            properties.appendProperties(stack, player, tooltip);
        }
    }
}
