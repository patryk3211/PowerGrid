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
package org.patryk3211.powergrid.chemistry.recipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import netscape.javascript.JSObject;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.reagent.ReagentIngredient;
import org.patryk3211.powergrid.chemistry.reagent.ReagentRegistry;
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;
import org.patryk3211.powergrid.chemistry.recipe.condition.IReactionCondition;
import org.patryk3211.powergrid.chemistry.recipe.equation.IReactionEquation;

import java.util.Optional;

public class ReactionRecipeSerializer implements RecipeSerializer<ReactionRecipe> {
    public static final Identifier ID = new Identifier(PowerGrid.MOD_ID, "reaction");
    public static final ReactionRecipeSerializer INSTANCE = new ReactionRecipeSerializer();

    public static final Codec<ReagentStack> RESULT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ReagentRegistry.REGISTRY.getCodec().fieldOf("reagent").forGetter(ReagentStack::getReagent),
            Codec.optionalField("amount", Codec.INT).forGetter(r -> r.getAmount() == 1 ? Optional.empty() : Optional.of(r.getAmount()))
    ).apply(instance, (reagent, amount) -> new ReagentStack(reagent, amount.orElse(1))));

    public static final Codec<ReactionRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(ReagentIngredient.CODEC).fieldOf("ingredients").forGetter(ReactionRecipe::getReagentIngredients),
            Codec.list(RESULT_CODEC).fieldOf("results").forGetter(ReactionRecipe::getReagentResults),
            Codec.list(IReactionCondition.CODEC).fieldOf("conditions").forGetter(ReactionRecipe::getReactionConditions),
            Codec.optionalField("flags", Codec.list(ReactionFlag.CODEC)).forGetter(recipe -> {
                var flags = recipe.getFlagList();
                return flags.isEmpty() ? Optional.empty() : Optional.of(flags);
            }),
            Codec.FLOAT.fieldOf("energy").forGetter(ReactionRecipe::getReactionEnergy),
            IReactionEquation.CODEC.fieldOf("rate").forGetter(ReactionRecipe::getReactionRate)
//            Codec.FLOAT.fieldOf("rate").forGetter(ReactionRecipe::getReactionRate)
    ).apply(instance, (ingredients, results, conditions, flags, energy, rate) -> {
        var params = new ReactionRecipe.RecipeConstructorParameters();
        params.ingredients = ingredients;
        params.results = results;
        params.conditions = conditions;
        flags.ifPresent(flagList -> flagList.forEach(flag -> params.flags.set(flag.getBit())));
        params.energy = energy;
        params.rate = rate;
        return new ReactionRecipe(null, params);
    }));

    @Override
    public ReactionRecipe read(Identifier id, JsonObject json) {
        var recipe = CODEC.decode(JsonOps.INSTANCE, json).getOrThrow(false, PowerGrid.LOGGER::error).getFirst();
        recipe.setId(id);
        return recipe;
    }

    @Override
    public ReactionRecipe read(Identifier id, PacketByteBuf buf) {
        var recipe = buf.decodeAsJson(CODEC);
        recipe.setId(id);
        return recipe;
    }

    @Override
    public void write(PacketByteBuf buf, ReactionRecipe recipe) {
        buf.encodeAsJson(CODEC, recipe);
    }
}
