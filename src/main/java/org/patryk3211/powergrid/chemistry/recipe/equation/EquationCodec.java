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
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class EquationCodec implements Codec<IReactionEquation> {
    public static final Codec<IReactionEquation> CODEC = new EquationCodec();

    private EquationCodec() { }

    @Override
    public <T> DataResult<Pair<IReactionEquation, T>> decode(DynamicOps<T> ops, T input) {
        var asNumber = ops.getNumberValue(input);
        if(asNumber.result().isPresent()) {
            var decoded = ConstEquation.CODEC.decode(ops, input);
            return decoded.map(pair -> pair.mapFirst(eq -> eq));
        }

        var asMap = ops.getMap(input);
        if(asMap.result().isPresent()) {
            var decoded = MapAggregateEquation.CODEC.decode(ops, input);
            return decoded.map(pair -> pair.mapFirst(eq -> eq));
        }

        var asString = ops.getStringValue(input);
        if(asString.result().isPresent())
            return VarEquation.CODEC.decode(ops, input);
        return DataResult.error(() -> "Given element type is not supported");
    }

    @Override
    public <T> DataResult<T> encode(IReactionEquation equation, DynamicOps<T> ops, T input) {
        var type = (IReactionEquation.Type<IReactionEquation>) equation.getType();
        return type.codec.encode(equation, ops, input);
    }
}
