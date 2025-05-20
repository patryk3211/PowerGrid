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
package org.patryk3211.powergrid.chemistry.recipe;

import net.minecraft.util.StringIdentifiable;

public enum ReactionFlag implements StringIdentifiable {
    COMBUSTION("combustion", 0);

    public static final com.mojang.serialization.Codec<ReactionFlag> CODEC = StringIdentifiable.createCodec(ReactionFlag::values);

    private final String name;
    private final int bit;

    ReactionFlag(String name, int bit) {
        this.name = name;
        this.bit = bit;
    }

    @Override
    public String asString() {
        return name;
    }

    public int getBit() {
        return bit;
    }

    public static ReactionFlag fromString(String name) {
        return switch(name) {
            case "combustion" -> COMBUSTION;
            default -> throw new IllegalArgumentException("Unknown reaction flag name '" + name + "'");
        };
    }
}
