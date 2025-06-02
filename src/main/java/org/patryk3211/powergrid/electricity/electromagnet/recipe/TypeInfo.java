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
package org.patryk3211.powergrid.electricity.electromagnet.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;

public class TypeInfo implements IRecipeTypeInfo {
    public static final Identifier ID = new Identifier(PowerGrid.MOD_ID, "magnetization");
    public static final RecipeType<MagnetizingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return ID.toString();
        }
    };

    public static final RecipeSerializer<MagnetizingRecipe> SERIALIZER = new ProcessingRecipeSerializer<>(MagnetizingRecipe::new);

    TypeInfo() { }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public <T extends RecipeSerializer<?>> T getSerializer() {
        return (T) SERIALIZER;
    }

    @Override
    public <T extends RecipeType<?>> T getType() {
        return (T) TYPE;
    }
}
