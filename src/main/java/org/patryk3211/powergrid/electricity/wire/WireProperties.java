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
package org.patryk3211.powergrid.electricity.wire;

import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

public class WireProperties {
    public static <I extends WireItem, P> NonNullUnaryOperator<ItemBuilder<I, P>> setResistance(float resistance) {
        return b -> {
            b.onRegister(item -> item.resistance = resistance);
            return b;
        };
    }

    public static <I extends WireItem, P> NonNullUnaryOperator<ItemBuilder<I, P>> setMaxLength(float length) {
        return b -> {
            b.onRegister(item -> item.maxLength = length);
            return b;
        };
    }

    public static <I extends WireItem, P> NonNullUnaryOperator<ItemBuilder<I, P>> setAll(float resistance, float length) {
        return b -> {
            b.onRegister(item -> {
                item.resistance = resistance;
                item.maxLength = length;
            });
            return b;
        };
    }
}
