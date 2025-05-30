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

public class RecipeCatalyzerCondition implements IReactionCondition {
    public static final Codec<RecipeCatalyzerCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("strength").forGetter(RecipeCatalyzerCondition::getStrength)
    ).apply(instance, RecipeCatalyzerCondition::new));
    public static final Type<RecipeCatalyzerCondition> TYPE = new Type<>("catalyzer", CODEC);

    private final float strength;

    public RecipeCatalyzerCondition(float strength) {
        this.strength = strength;
    }

    public float getStrength() {
        return strength;
    }

    @Override
    public Type<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(ReagentConditions conditions) {
        return conditions.catalyzer() >= strength;
    }
}
