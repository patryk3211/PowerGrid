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
package org.patryk3211.powergrid.chemistry.reagent;

import com.tterrag.registrate.util.nullness.NonNullFunction;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.recipe.RegistrateReactionRecipeProvider;

import java.util.function.Consumer;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ReagentTripletBuilder<S extends Reagent, L extends Reagent, G extends Reagent> {
    public static final int STATE_CHANGE_RATE = 100;

    private final String name;
    private final Reagent.Properties properties;
    private final ReagentBuilder<S, ?> solidBuilder;
    private final ReagentBuilder<L, ?> liquidBuilder;
    private final ReagentBuilder<G, ?> gasBuilder;

    private ReagentTripletBuilder(String name, Reagent.Properties properties, NonNullFunction<Reagent.Properties, S> solidConstructor, NonNullFunction<Reagent.Properties, L> liquidConstructor, NonNullFunction<Reagent.Properties, G> gasConstructor) {
        this.name = name;
        this.properties = properties;
        solidBuilder = REGISTRATE.reagent("solid_" + name, solidConstructor)
                .initialProperties(properties)
                .onRegister(r -> r.withFixedState(ReagentState.SOLID));
        liquidBuilder = REGISTRATE.reagent("liquid_" + name, liquidConstructor)
                .initialProperties(properties)
                .onRegister(r -> r.withFixedState(ReagentState.LIQUID));
        gasBuilder = REGISTRATE.reagent(name + "_gas", gasConstructor)
                .initialProperties(properties)
                .onRegister(r -> r.withFixedState(ReagentState.GAS));
    }

    public ReagentTripletBuilder<S, L, G> solid(Consumer<ReagentBuilder<S, ?>> consumer) {
        consumer.accept(solidBuilder);
        return this;
    }

    public ReagentTripletBuilder<S, L, G> liquid(Consumer<ReagentBuilder<L, ?>> consumer) {
        consumer.accept(liquidBuilder);
        return this;
    }

    public ReagentTripletBuilder<S, L, G> gas(Consumer<ReagentBuilder<G, ?>> consumer) {
        consumer.accept(gasBuilder);
        return this;
    }

    public ReagentTripletBuilder<S, L, G> simpleLang(String name) {
        solidBuilder.lang(name);
        liquidBuilder.lang(name);
        gasBuilder.lang(name);
        return this;
    }

    public ReagentTripletBuilder<S, L, G> recipes(float liquifyEnergy, float vaporizeEnergy) {
        solidBuilder.recipe((ctx, prov) -> {
            // Solidify
            RegistrateReactionRecipeProvider.reaction(prov, PowerGrid.asResource("state/" + name + "_solidify"), b -> b
                    .result(ctx.get(), 1)
                    .ingredient(liquidBuilder.getEntry(), 1)
                    .maximumTemperatureCondition(properties.getMeltingPoint())
                    .energy(liquifyEnergy)
                    .rate(STATE_CHANGE_RATE));
        });
        liquidBuilder.recipe((ctx, prov) -> {
            // Melt
            RegistrateReactionRecipeProvider.reaction(prov, PowerGrid.asResource("state/" + name + "_liquify"), b -> b
                    .result(ctx.get(), 1)
                    .ingredient(solidBuilder.getEntry(), 1)
                    .minimumTemperatureCondition(properties.getMeltingPoint())
                    .energy(-liquifyEnergy)
                    .rate(STATE_CHANGE_RATE));
            // Condense
            RegistrateReactionRecipeProvider.reaction(prov, PowerGrid.asResource("state/" + name + "_condense"), b -> b
                    .result(ctx.get(), 1)
                    .ingredient(gasBuilder.getEntry(), 1)
                    .maximumTemperatureCondition(properties.getBoilingPoint())
                    .energy(vaporizeEnergy)
                    .rate(STATE_CHANGE_RATE));
        });
        gasBuilder.recipe((ctx, prov) -> {
            // Vaporize
            RegistrateReactionRecipeProvider.reaction(prov, PowerGrid.asResource("state/" + name + "_vaporize"), b -> b
                    .result(ctx.get(), 1)
                    .ingredient(liquidBuilder.getEntry(), 1)
                    .minimumTemperatureCondition(properties.getBoilingPoint())
                    .energy(-vaporizeEnergy)
                    .rate(STATE_CHANGE_RATE));
        });
        return this;
    }

    public ReagentTriplet<S, L, G> register() {
        return new ReagentTriplet<>(solidBuilder.register(), liquidBuilder.register(), gasBuilder.register());
    }

    static ReagentTripletBuilder<Reagent, Reagent, Reagent> create(String name, Reagent.Properties properties) {
        return new ReagentTripletBuilder<>(name, properties, Reagent::new, Reagent::new, Reagent::new);
    }
}
