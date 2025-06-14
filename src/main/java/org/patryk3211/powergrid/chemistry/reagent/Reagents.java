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

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import org.patryk3211.powergrid.PowerGridRegistrate;
import org.patryk3211.powergrid.chemistry.recipe.RegistrateReactionRecipeProvider;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.function.Supplier;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class Reagents {
    public static final Reagent EMPTY = ReagentRegistry.DEFAULT;

    public static final ReagentEntry<Reagent> OXYGEN = REGISTRATE.reagent("oxygen", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(-218.8f)
                    .boilingPoint(-182.9f)
                    .heatCapacity(29.37f))
            .register();
    public static final ReagentEntry<Reagent> HYDROGEN = REGISTRATE.reagent("hydrogen", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(-259.2f)
                    .boilingPoint(-252.8f)
                    .heatCapacity(28.84f))
            .register();
    public static final ReagentTriplet<Reagent, Reagent, Reagent> WATER =
            ReagentTripletBuilder.create("water", simpleProperties(0.0f, 100.0f, 75.38f))
                    .gas(b -> b.lang("Steam").particleColor(0xEEEEEE))
                    .liquid(b -> b.lang("Water").fluid(Fluids.WATER, 22f))
                    .solid(b -> b.lang("Ice").item(Items.ICE, Reagent.BLOCK_MOLE_AMOUNT, -10f))
                    .recipes(333.55f, 40.65f)
                    .liquidConductance(0.005f)
                    .register();
    public static final ReagentEntry<Reagent> NITROGEN = REGISTRATE.reagent("nitrogen", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(-209.8f)
                    .boilingPoint(-195.7f)
                    .heatCapacity(29.12f))
            .register();
    public static final ReagentEntry<Reagent> SULFUR = REGISTRATE.reagent("sulfur", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(115.2f)
                    .boilingPoint(444.6f)
                    .heatCapacity(22.75f))
            .item(ModdedItems.SULFUR, 1000, 22f)
            .register();
    public static final ReagentEntry<Reagent> SULFUR_DIOXIDE = REGISTRATE.reagent("sulfur_dioxide", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(-72.0f)
                    .boilingPoint(10.0f)
                    .heatCapacity(42.5f))
            .particleColor(0xDDDDDD)
            .register();
    public static final ReagentEntry<Reagent> SULFUR_TRIOXIDE = simpleReagent("sulfur_trioxide", 16.9f, 45.0f, 61.5f)
            .particleColor(0xDDDDDD)
            .register();
    public static final ReagentEntry<Reagent> SULFURIC_ACID = simpleReagent("sulfuric_acid", 10.3f, 337.0f, 135.8f)
            .simpleFluid(0xFFFFEE80, 22f)
            .register();

    public static final ReagentEntry<Reagent> REDSTONE = simpleReagent("redstone", 325f, 452f, 53.4f)
            .item(Items.REDSTONE, 1000, 22f)
            .register();

    public static final ReagentEntry<Reagent> REDSTONE_SULFATE = simpleReagent("redstone_sulfate", 236f, 352f, 174.2f)
            .register();

    public static final ReagentEntry<Reagent> DISSOLVED_REDSTONE_SULFATE = dissolvedReagent("dissolved_redstone_sulfate", REDSTONE_SULFATE, WATER.liquid())
            .simpleFluid(0xFFFF2020, 22f)
            .liquidConductance(0.2f)
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }

    private static ReagentBuilder<Reagent, PowerGridRegistrate> simpleReagent(String name, float meltingPoint, float boilingPoint, float heatCapacity) {
        return REGISTRATE.reagent(name, Reagent::new)
                .properties(properties -> properties
                        .meltingPoint(meltingPoint)
                        .boilingPoint(boilingPoint)
                        .heatCapacity(heatCapacity));
    }

    public static ReagentBuilder<Reagent, PowerGridRegistrate> dissolvedReagent(String name, Supplier<Reagent> dissolved, Supplier<Reagent> dissolver) {
        return REGISTRATE.reagent(name, Reagent::new)
                .initialProperties(dissolver)
                .fixedState(ReagentState.LIQUID)
                .properties(properties -> properties.heatCapacity(dissolved.get().getHeatCapacity() + dissolver.get().getHeatCapacity()))
                .recipe((ctx, prov) -> dissolvedRecipes(ctx, prov, dissolved, dissolver));
    }

    public static <T extends Reagent> void dissolvedRecipes(DataGenContext<Reagent, T> ctx, RegistrateRecipeProvider prov, Supplier<? extends Reagent> dissolved, Supplier<? extends Reagent> dissolver) {
        var dissolverR = dissolver.get();
        // Dissolving reaction
        RegistrateReactionRecipeProvider.reaction(prov, ctx.getId(), b -> b
                .result(ctx.getEntry(), 1)
                .ingredient(dissolved.get(), 1)
                .ingredient(dissolverR, 1)
                .temperatureCondition(dissolverR.getMeltingPoint(), dissolverR.getBoilingPoint())
                .suffix("_dissolve")
                .energy(-5)
                .rate(10)
        );
        // Boiling reaction
        RegistrateReactionRecipeProvider.reaction(prov, ctx.getId(), b -> b
                .ingredient(ctx.getEntry(), 1)
                .result(dissolved.get(), 1)
                .result(dissolverR, 1)
                .minimumTemperatureCondition(dissolverR.getBoilingPoint())
                .suffix("_boil")
                .energy(5)
                .rate(10)
        );
    }

    public static Reagent.Properties simpleProperties(float meltingPoint, float boilingPoint, float heatCapacity) {
        return new Reagent.Properties()
                .meltingPoint(meltingPoint)
                .boilingPoint(boilingPoint)
                .heatCapacity(heatCapacity);
    }
}
