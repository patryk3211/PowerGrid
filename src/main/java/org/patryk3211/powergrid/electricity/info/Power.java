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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class Power {
    public static void rated(float value, PlayerEntity player, List<Text> tooltip) {
        Lang.translate("tooltip.power.rated")
                .style(Formatting.GRAY).addTo(tooltip);
        Lang.builder()
                .add(Text.of(" ")).add(Lang.number(value))
                .add(Text.of(" ")).add(Unit.POWER.get())
                .style(Formatting.YELLOW).addTo(tooltip);
    }
}
