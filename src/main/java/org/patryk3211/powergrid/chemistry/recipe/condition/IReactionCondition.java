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
import org.patryk3211.powergrid.chemistry.recipe.ReagentConditions;

import java.util.function.Predicate;

public interface IReactionCondition extends Predicate<ReagentConditions> {
    Codec<IReactionCondition> CODEC = ReactionConditionCodec.CODEC;

    Type<?> getType();

    class Type<T extends IReactionCondition> {
        public final String name;
        public final Codec<T> codec;

        public Type(String name, Codec<T> codec) {
            this.name = name;
            this.codec = codec;
        }
    }
}
