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
package org.patryk3211.powergrid.utility;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.patryk3211.powergrid.collections.ModdedTags;

public class RecipeNbtTransfer {
    public static void transfer(Iterable<ItemStack> inputs, Iterable<ItemStack> outputs) {
        CompoundTag schematic = null;
        for(var consumed : inputs) {
            if(consumed.is(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag) && consumed.has(DataComponents.CUSTOM_DATA)) {
                var root = consumed.get(DataComponents.CUSTOM_DATA).copyTag();
                if(root.contains("Schematic")) {
                    schematic = consumed.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("Schematic").copy();
                }
            }
        }
        if(schematic == null)
            return;
        for(var output : outputs) {
            if(output.is(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)) {
                var tag = new CompoundTag();
                tag.put("Schematic", schematic);
                output.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }
}
