/*
 * Copyright 2026 patryk3211
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
package org.patryk3211.powergrid.electricity.light.string;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.DyeColor;

import java.util.List;

public record PatternData(DyeColor[] colors) {
    public static final Codec<PatternData> CODEC = Codec.list(DyeColor.CODEC, 1, 8)
            .xmap(
                    list -> new PatternData(list.toArray(DyeColor[]::new)),
                    pattern -> List.of(pattern.colors)
            );

    public static PatternData of(List<DyeColor> colors) {
        return new PatternData(colors.toArray(DyeColor[]::new));
    }
}
