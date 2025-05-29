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

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;

import java.util.function.UnaryOperator;

public class RegistrateReactionRecipeProvider {
    public static void reaction(RegistrateRecipeProvider provider, Identifier id, UnaryOperator<ReactionJsonBuilder> builder) {
        builder.apply(new ReactionJsonBuilder(id)).offerTo(provider);
    }

    public static <T extends Reagent> void reaction(RegistrateRecipeProvider provider, DataGenContext<Reagent, T> ctx, int amount, UnaryOperator<ReactionJsonBuilder> builder) {
        builder.apply(ReactionJsonBuilder.create(ctx, amount)).offerTo(provider);
    }
}
