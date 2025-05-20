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

import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;

import java.util.HashMap;
import java.util.Map;

public class EquationVar {
    public static final Map<String, Type<?>> VARIABLE_TYPES = new HashMap<>();

    public EquationVar() {
    }

    public static abstract class Type<T extends EquationVar> {
        public Type(String name) {
            if(VARIABLE_TYPES.containsKey(name))
                throw new IllegalArgumentException("Duplicate variable type name");
            VARIABLE_TYPES.put(name, this);
        }

        public abstract void read(JsonObject json);
        public abstract void read(PacketByteBuf buf);
        public abstract void write(PacketByteBuf buf);
    }
}
