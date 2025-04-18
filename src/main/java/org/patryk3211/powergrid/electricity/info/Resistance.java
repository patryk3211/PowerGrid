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
import com.simibubi.create.foundation.utility.LangBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class Resistance {
    public static void series(float value, PlayerEntity player, List<Text> tooltip) {
        if(value == 0)
            return;
        boolean hasGoggles = GogglesItem.isWearingGoggles(player);
        if(!hasGoggles)
            return;

        LangBuilder unit = Lang.translate("generic.unit.ohm");
        Lang.translate("tooltip.resistance.series")
                .style(Formatting.GRAY).addTo(tooltip);
        LangBuilder valueText = Lang.builder().add(Text.of(" "));
        if(value < 1) {
            // Millis
            valueText.add(Lang.number(value * 1000))
                    .add(Text.of(" m"))
                    .add(unit);
        } else if(value < 1000) {
            valueText.add(Lang.number(value))
                    .add(Text.of(" "))
                    .add(unit);
        } else if(value < 1000000) {
            valueText.add(Lang.number(value / 1000))
                    .add(Text.of(" k"))
                    .add(unit);
        } else {
            valueText.add(Lang.number(value / 1000000))
                    .add(Text.of(" M"))
                    .add(unit);
        }
        valueText.style(Formatting.DARK_AQUA).addTo(tooltip);
    }
}
