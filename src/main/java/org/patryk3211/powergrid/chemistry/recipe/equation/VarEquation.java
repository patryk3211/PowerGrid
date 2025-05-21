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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class VarEquation {
    private static final Map<String, Pair<Function<String, IReactionEquation>, Function<IReactionEquation, String>>> CODECS = new HashMap<>();

    public static final Codec<IReactionEquation> CODEC = Codec.STRING.xmap(str -> {
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
        var type = eq.getType();
        var from = CODECS.get(type.name).getSecond();
        return from == null ? type.name : type.name + "#" + from.apply(eq);
    });

    static {
        var tInst = new TemperatureEquation();
        Pair<Function<String, IReactionEquation>, Function<IReactionEquation, String>> t =
                Pair.of($ -> tInst, null);
        CODECS.put("temperature", t);
        CODECS.put("temp", t);
        CODECS.put("T", t);

        Pair<Function<String, IReactionEquation>, Function<IReactionEquation, String>> p =
                Pair.of(ConcentrationEquation::new, eq -> ((ConcentrationEquation) eq).getArg());
        CODECS.put("concentration", p);
        CODECS.put("Conc", p);

        var catInst = new CatalyzerEquation();
        Pair<Function<String, IReactionEquation>, Function<IReactionEquation, String>> cat =
                Pair.of($ -> catInst, null);
        CODECS.put("catalyzerStrength", cat);
        CODECS.put("catalyzer", cat);
        CODECS.put("Cat", cat);
    }
}
