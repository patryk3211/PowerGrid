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
package org.patryk3211.powergrid.chemistry.recipe.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentRegistry;
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;
import org.patryk3211.powergrid.chemistry.recipe.ReagentConditions;

import java.util.Optional;

public class RecipeConcentrationCondition implements IReactionCondition {
    public static final Codec<RecipeConcentrationCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ReagentRegistry.REGISTRY.getCodec().fieldOf("reagent").forGetter(RecipeConcentrationCondition::getReagent),
            Codec.optionalField("min", Codec.FLOAT).forGetter(RecipeConcentrationCondition::getMin),
            Codec.optionalField("max", Codec.FLOAT).forGetter(RecipeConcentrationCondition::getMax),
            Codec.optionalField("in", ReagentState.CODEC).forGetter(RecipeConcentrationCondition::getState)
    ).apply(instance, RecipeConcentrationCondition::new));

    public static final Type<RecipeConcentrationCondition> TYPE = new Type<>("concentration", CODEC);

    private final Reagent reagent;
    private final Float min;
    private final Float max;

    private final ReagentState inState;

    public RecipeConcentrationCondition(Reagent reagent, Optional<Float> min, Optional<Float> max, Optional<ReagentState> state) {
        this.reagent = reagent;
        this.min = min.orElse(null);
        this.max = max.orElse(null);
        this.inState = state.orElse(null);

        if(min.isPresent() && max.isPresent()) {
            if(min.get() > max.get()) {
                throw new IllegalArgumentException("Minimum reagent concentration must be smaller than maximum concentration");
            }
        }
        if(min.isPresent() && (min.get() < 0 || min.get() > 1)) {
            throw new IllegalArgumentException("Minimum reagent concentration must be in [0; 1] range");
        }
        if(max.isPresent() && (max.get() < 0 || max.get() > 1)) {
            throw new IllegalArgumentException("Maximum reagent concentration must be in [0; 1] range");
        }
        if(min.isEmpty() && max.isEmpty()) {
            throw new IllegalStateException("Empty concentration condition");
        }
    }

    @Override
    public Type<RecipeConcentrationCondition> getType() {
        return TYPE;
    }

    public Reagent getReagent() {
        return reagent;
    }

    public Optional<Float> getMin() {
        return Optional.ofNullable(min);
    }

    public Optional<Float> getMax() {
        return Optional.ofNullable(max);
    }

    public Optional<ReagentState> getState() {
        return Optional.ofNullable(inState);
    }

    @Override
    public boolean test(ReagentConditions conditions) {
        var concentration = inState == null ? conditions.concentration(reagent) : conditions.concentration(reagent, inState);
        if(min != null && concentration < min)
            return false;
        if(max != null && concentration > max)
            return false;
        return true;
    }
}
