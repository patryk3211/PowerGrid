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
package org.patryk3211.powergrid.config;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.wire.WireItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.DoubleSupplier;

public class WireValues {
    private static final List<Provider> providers = new ArrayList<>();

    public static float resistance(WireItem wire) {
        return get(wire, Provider::resistance);
    }

    public static float maxLength(WireItem wire) {
        return get(wire, Provider::maxLength);
    }

    public static float thermalMass(WireItem wire) {
        return get(wire, Provider::thermalMass);
    }

    public static float dissipationFactor(WireItem wire) {
        return get(wire, Provider::dissipationFactor);
    }

    private static float get(WireItem wire, BiFunction<Provider, WireItem, DoubleSupplier> map) {
        for(var provider : providers) {
            var v = map.apply(provider, wire);
            if(v != null)
                return (float) v.getAsDouble();
        }
        throw new IllegalArgumentException("Wire '" + wire + "' not found in any provider");
    }

    public static void register(Provider provider) {
        providers.add(provider);
    }

    public interface Provider {
        @Nullable
        DoubleSupplier resistance(WireItem wire);
        @Nullable
        DoubleSupplier maxLength(WireItem wire);
        @Nullable
        DoubleSupplier thermalMass(WireItem wire);
        @Nullable
        DoubleSupplier dissipationFactor(WireItem wire);
    }
}
