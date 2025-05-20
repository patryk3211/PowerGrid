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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAggregateEquation extends AggregateEquation {
    private static final Map<String, Codec<? extends IReactionEquation>> CODECS = new HashMap<>();

    public static final Codec<MapAggregateEquation> CODEC = Codec.of(new Encoder<>() {
        @Override
        public <T> DataResult<T> encode(MapAggregateEquation eq, DynamicOps<T> ops, T input) {
            var list = ops.createMap(eq.equations.stream().map(innerEquation -> {
                var type = (Type<IReactionEquation>) innerEquation.getType();
                var encoded = type.codec.encode(innerEquation, ops, input);
                if(encoded.result().isEmpty())
                    return null;
                return Pair.of(ops.createString(type.name), encoded.result().get());
            }));
            return DataResult.success(list);
        }
    }, new Decoder<>() {
        @Override
        public <T> DataResult<Pair<MapAggregateEquation, T>> decode(DynamicOps<T> ops, T input) {
            var asMap = ops.getMap(input);
            if(asMap.result().isEmpty())
                return DataResult.error(() -> "Not a map");

            var equations = new ArrayList<IReactionEquation>();
            asMap.result().get().entries().forEach(entry -> {
                var key = ops.getStringValue(entry.getFirst());
                if(key.result().isEmpty())
                    return;
                var op = CODECS.get(key.result().get());
                var decoded = op.decode(ops, entry.getSecond());
                if(decoded.result().isEmpty())
                    return;
                equations.add(decoded.result().get().getFirst());
            });
            return DataResult.success(Pair.of(new MapAggregateEquation(equations), input));
        }
    });

    public static final Type<MapAggregateEquation> TYPE = new Type<>("opmap", CODEC);

    public MapAggregateEquation(List<IReactionEquation> equations) {
        super(equations);
    }

    @Override
    public float evaluate(ReagentConditions conditions) {
        float value = 0;
        for(var equation : equations)
            value += equation.evaluate(conditions);
        return value;
    }

    @Override
    public Type<?> getType() {
        return TYPE;
    }

    private static void registerOp(IReactionEquation.Type<?> type) {
        CODECS.put(type.name, type.codec);
    }

    static {
        registerOp(AddEquation.TYPE);
        registerOp(SubtractEquation.TYPE);
        registerOp(MultiplyEquation.TYPE);
        registerOp(DivideEquation.TYPE);
        registerOp(MinEquation.TYPE);
        registerOp(MaxEquation.TYPE);
    }
}
