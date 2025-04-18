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

import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.LinkedList;
import java.util.List;

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

    @Override
    public void modify(ItemStack itemStack, PlayerEntity playerEntity, TooltipContext tooltipContext, List<Text> tooltip) {
        List<Text> lines = new LinkedList<>();
        properties.appendProperties(itemStack, playerEntity, lines);
        if(!lines.isEmpty()) {
            tooltip.add(Components.immutableEmpty());
            tooltip.addAll(lines);
        }
    }
}
