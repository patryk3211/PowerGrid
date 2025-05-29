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

import com.mojang.serialization.Codec;
import org.patryk3211.powergrid.chemistry.recipe.ReagentConditions;

import java.util.List;

public class SubtractEquation extends AggregateEquation {
    public static final Codec<SubtractEquation> CODEC = AggregateEquation.CODEC.xmap(SubtractEquation::new, SubtractEquation::getEquations);
    public static final Type<SubtractEquation> TYPE = new Type<>("subtract", CODEC);

    public SubtractEquation(List<IReactionEquation> equations) {
        super(equations);
    }

    @Override
    public float evaluate(ReagentConditions conditions) {
        float result = 0;
        boolean first = true;
        for(var equation : equations) {
            var value = equation.evaluate(conditions);
            if(first) {
                result = value;
                first = false;
            } else {
                result -= value;
            }
        }
        return result;
    }

    @Override
    public Type<?> getType() {
        return TYPE;
    }
}
