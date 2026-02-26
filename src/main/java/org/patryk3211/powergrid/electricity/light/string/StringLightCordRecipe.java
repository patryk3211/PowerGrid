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

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.ArrayList;

public class StringLightCordRecipe extends CustomRecipe {
    public static final RecipeSerializer<StringLightCordRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(StringLightCordRecipe::new);

    public StringLightCordRecipe(CraftingBookCategory category) {
        super(category);
    }


    @Override
    public boolean matches(CraftingInput input, Level level) {
        var hasCord = false;

        for(var stack : input.items()) {
            if(ModdedItems.STRING_LIGHT_CORD.isIn(stack)) {
                if(hasCord)
                    return false;
                hasCord = true;
            } else if(!stack.isEmpty() && !(stack.getItem() instanceof DyeItem)) {
                return false;
            }
        }

        return hasCord;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        var result = ModdedItems.STRING_LIGHT_CORD.asStack();
        var colors = new ArrayList<Byte>();

        for(var stack : input.items()) {
            if(stack.getItem() instanceof DyeItem dye) {
                colors.add((byte) dye.getDyeColor().ordinal());
            }
        }

        var tag = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putByteArray("Pattern", colors);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return result;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ModdedItems.STRING_LIGHT_CORD.asStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width > 0 && height > 0;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
