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

import java.util.HashMap;
import java.util.Map;

public class ReactionConditionCodec {
    private static final Map<String, IReactionCondition.Type<IReactionCondition>> TYPES = new HashMap<>();

    public static final Codec<IReactionCondition> CODEC = Codec.STRING.dispatch(
            condition -> condition.getType().name,
            type -> TYPES.get(type).codec
    );

    private static <T extends IReactionCondition> void addConditionType(IReactionCondition.Type<T> type) {
        TYPES.put(type.name, (IReactionCondition.Type<IReactionCondition>) type);
    }

    static {
        addConditionType(RecipeConcentrationCondition.TYPE);
        addConditionType(RecipeTemperatureCondition.TYPE);
        addConditionType(RecipeCatalyzerCondition.TYPE);
    }
}
