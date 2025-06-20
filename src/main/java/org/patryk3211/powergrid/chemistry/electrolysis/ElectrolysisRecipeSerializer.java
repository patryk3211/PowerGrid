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
package org.patryk3211.powergrid.chemistry.electrolysis;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.reagent.ReagentIngredient;

public class ElectrolysisRecipeSerializer implements RecipeSerializer<ElectrolysisRecipe> {
    public static final Codec<ElectrolysisRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(ReagentIngredient.CODEC).fieldOf("ingredients").forGetter(ElectrolysisRecipe::getReagentIngredients),
            Codec.list(ElectrolysisResult.CODEC).fieldOf("results").forGetter(ElectrolysisRecipe::getReagentResults),
            Codec.FLOAT.fieldOf("minimumPotential").forGetter(ElectrolysisRecipe::getMinimumPotential)
    ).apply(instance, ElectrolysisRecipe::new));

    public static final ElectrolysisRecipeSerializer INSTANCE = new ElectrolysisRecipeSerializer();

    private ElectrolysisRecipeSerializer() {

    }

    @Override
    public ElectrolysisRecipe read(Identifier id, JsonObject json) {
        var recipe = CODEC.decode(JsonOps.INSTANCE, json).getOrThrow(false, PowerGrid.LOGGER::error).getFirst();
        recipe.setId(id);
        return recipe;
    }

    @Override
    public ElectrolysisRecipe read(Identifier id, PacketByteBuf buf) {
        var recipe = buf.decodeAsJson(CODEC);
        recipe.setId(id);
        return recipe;
    }

    @Override
    public void write(PacketByteBuf buf, ElectrolysisRecipe recipe) {
        buf.encodeAsJson(CODEC, recipe);
    }
}
