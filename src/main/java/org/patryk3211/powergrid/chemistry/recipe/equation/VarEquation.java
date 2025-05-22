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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class VarEquation {
    private static final Map<String, Pair<Function<String, IReactionEquation>, Function<IReactionEquation, String>>> CODECS = new HashMap<>();

    public static final Codec<IReactionEquation> CODEC = Codec.STRING.xmap(str -> {
        prepare();
        int index = str.indexOf('#');
        String name, arg;
        if (index == -1) {
            name = str;
            arg = "";
        } else {
            name = str.substring(0, index);
            arg = str.substring(index + 1);
        }
        return CODECS.get(name).getFirst().apply(arg);
    }, eq -> {
        prepare();
        var type = eq.getType();
        var from = CODECS.get(type.name).getSecond();
        return from == null ? type.name : type.name + "#" + from.apply(eq);
    });

    private static void add(IReactionEquation.Type<?> type, Function<String, IReactionEquation> to, @Nullable Function<IReactionEquation, String> from, String... aliases) {
        if(type.codec != CODEC)
            throw new IllegalArgumentException("Variable equation types must have the VarEquation codec");
        var pair = Pair.of(to, from);

        CODECS.put(type.name, pair);
        for(var alias : aliases)
            CODECS.put(alias, pair);
    }

    private static void prepare() {
        if(CODECS.isEmpty()) {
            var tInst = new TemperatureEquation();
            add(TemperatureEquation.TYPE, $ -> tInst, null, "temperature", "temp");

            add(ConcentrationEquation.TYPE, ConcentrationEquation::new, eq -> ((ConcentrationEquation) eq).getArg(), "concentration");

            var catInst = new CatalyzerEquation();
            add(CatalyzerEquation.TYPE, $ -> catInst, null, "catalyzerStrength", "catalyzer");
        }
    }
}
