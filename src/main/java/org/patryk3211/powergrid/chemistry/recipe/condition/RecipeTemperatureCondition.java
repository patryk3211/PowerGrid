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
import org.patryk3211.powergrid.chemistry.recipe.ReagentConditions;

import java.util.Optional;

public class RecipeTemperatureCondition implements IReactionCondition {
    public static final Codec<RecipeTemperatureCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.optionalField("min", Codec.FLOAT).forGetter(RecipeTemperatureCondition::getMin),
            Codec.optionalField("max", Codec.FLOAT).forGetter(RecipeTemperatureCondition::getMax)
    ).apply(instance, RecipeTemperatureCondition::new));
    public static final Type<RecipeTemperatureCondition> TYPE = new Type<>("temperature", CODEC);

    private final Float min;
    private final Float max;

    public RecipeTemperatureCondition(Optional<Float> min, Optional<Float> max) {
        this.min = min.orElse(null);
        this.max = max.orElse(null);
    }

    @Override
    public boolean test(ReagentConditions conditions) {
        var temperature = conditions.temperature();
        if(temperature < min)
            return false;
        if(max != null) {
            return temperature < max;
        } else {
            return true;
        }
    }

    @Override
    public Type<?> getType() {
        return TYPE;
    }

    public Optional<Float> getMin() {
        return Optional.ofNullable(min);
    }

    public Optional<Float> getMax() {
        return Optional.ofNullable(max);
    }
}
