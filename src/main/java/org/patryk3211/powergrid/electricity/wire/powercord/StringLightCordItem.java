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
package org.patryk3211.powergrid.electricity.wire.powercord;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Voltage;

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
}
