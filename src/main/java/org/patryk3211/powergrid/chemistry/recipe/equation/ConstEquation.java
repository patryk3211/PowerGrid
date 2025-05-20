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
package org.patryk3211.powergrid.chemistry.recipe.equation;

import com.mojang.serialization.*;
import org.patryk3211.powergrid.chemistry.recipe.ReagentConditions;

public class ConstEquation implements IReactionEquation {
    public static final Codec<ConstEquation> CODEC = Codec.FLOAT.xmap(ConstEquation::new, ConstEquation::getValue);

    public static final Type<ConstEquation> TYPE = new Type<>("const", CODEC);

    private final float value;

    public ConstEquation(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    @Override
    public float evaluate(ReagentConditions conditions) {
        return value;
    }

    @Override
    public Type<ConstEquation> getType() {
        return TYPE;
    }
}
