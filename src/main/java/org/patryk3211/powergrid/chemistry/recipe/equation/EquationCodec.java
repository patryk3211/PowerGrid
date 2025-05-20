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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EquationCodec implements Codec<ReactionEquation> {
    private static final Map<String, Codec<? extends ReactionEquation>> EQUATION_OPS = new HashMap<>();
    private static final Map<String, Function<String, ReactionEquation>> EQUATION_VARIABLES = new HashMap<>();

    public static final Codec<ReactionEquation> CODEC = new EquationCodec();
    public static final Codec<List<ReactionEquation>> LIST_CODEC = Codec.list(CODEC);

    private EquationCodec() {

    }

    @Override
    public <T> DataResult<Pair<ReactionEquation, T>> decode(DynamicOps<T> ops, T input) {
        var asNumber = ops.getNumberValue(input);
        if(asNumber.result().isPresent())
            return DataResult.success(Pair.of(new ConstEquation(asNumber.result().get().floatValue()), input));
        var asMap = ops.getMap(input);
        if(asMap.result().isPresent()) {
            var map = asMap.result().get();
            var equations = new ArrayList<ReactionEquation>();
            map.entries().forEach(entry -> {
                var key = ops.getStringValue(entry.getFirst());
                if(key.result().isEmpty())
                    return;
                var op = EQUATION_OPS.get(key.result().get());
                var decoded = op.decode(ops, entry.getSecond());
                if(decoded.result().isEmpty())
                    return;
                equations.add(decoded.result().get().getFirst());
            });
            return DataResult.success(Pair.of(new MapAggregateEquation(equations), input));
        }
        var asString = ops.getStringValue(input);
        if(asString.result().isPresent()) {
            var str = asString.result().get();
            var idx = str.indexOf('#');
            String name, arg;
            if(idx == -1) {
                name = str;
                arg = "";
            } else {
                name = str.substring(0, idx);
                arg = str.substring(idx + 1);
            }
            var variableFunc = EQUATION_VARIABLES.get(name);
            if(variableFunc == null)
                return DataResult.error(() -> "Unknown variable name");
            return DataResult.success(Pair.of(variableFunc.apply(arg), input));
        }
        return DataResult.error(() -> "Given element type is not supported");
    }

    @Override
    public <T> DataResult<T> encode(ReactionEquation equation, DynamicOps<T> ops, T input) {
        if(equation instanceof ConstEquation eq) {
            return DataResult.success(ops.createFloat(eq.getValue()));
        }
        if(equation instanceof TemperatureEquation) {
            return DataResult.success(ops.createString("T"));
        }
        if(equation instanceof ConcentrationEquation eq) {
            return DataResult.success(ops.createString("Conc#" + eq.getArg()));
        }
        if(equation instanceof MultiplyEquation eq) {
            return MultiplyEquation.CODEC.encode(eq, ops, input);
        }
        if(equation instanceof MapAggregateEquation eq) {
            var list = ops.createMap(eq.equations.stream().map(innerEquation -> {
                String type;
                if(innerEquation instanceof INamedEquationPart part)
                    type = part.name();
                else
                    return null;
                return Pair.of(ops.createString(type), encode(innerEquation, ops, input).result().get());
            }));
            return DataResult.success(list);
        }
        return DataResult.error(() -> "Given element type is not supported");
    }

    static {
        EQUATION_OPS.put("multiply", MultiplyEquation.CODEC);

        var t = new TemperatureEquation();
        EQUATION_VARIABLES.put("temperature", $ -> t);
        EQUATION_VARIABLES.put("temp", $ -> t);
        EQUATION_VARIABLES.put("T", $ -> t);

        EQUATION_VARIABLES.put("concentration", ConcentrationEquation::new);
        EQUATION_VARIABLES.put("Conc", ConcentrationEquation::new);
    }
}
