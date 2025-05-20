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

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import org.patryk3211.powergrid.chemistry.recipe.ReagentConditions;

import java.util.List;

public class MultiplyEquation extends AggregateEquation implements INamedEquationPart {
    public static final Codec<MultiplyEquation> CODEC = Codec.of(new Encoder<>() {
        @Override
        public <T> DataResult<T> encode(MultiplyEquation equation, DynamicOps<T> dynamicOps, T t) {
            return EquationCodec.LIST_CODEC.encode(equation.getEquations(), dynamicOps, t);
        }
    }, new Decoder<>() {
        @Override
        public <T> DataResult<Pair<MultiplyEquation, T>> decode(DynamicOps<T> dynamicOps, T t) {
            var list = EquationCodec.LIST_CODEC.decode(dynamicOps, t);
            if(list.result().isEmpty())
                return DataResult.error(() -> "Failed to decode list");
            return DataResult.success(Pair.of(new MultiplyEquation(list.result().get().getFirst()), t));
        }
    });

    public MultiplyEquation(List<ReactionEquation> equations) {
        super(equations);
    }

    @Override
    public float evaluate(ReagentConditions conditions) {
        float result = 1;
        for(var equation : equations) {
            result *= equation.evaluate(conditions);
        }
        return result;
    }

    @Override
    public String name() {
        return "multiply";
    }
}
