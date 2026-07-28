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
package org.patryk3211.powergrid.electricity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class RebuildBatchPlanner {
    private RebuildBatchPlanner() {
    }

    record Unit<K, V>(K key, Set<V> required) {
        Unit {
            required = Set.copyOf(required);
            if(required.isEmpty())
                throw new IllegalArgumentException("A rebuild unit must require at least one value");
        }
    }

    record Batch<K, V>(List<K> keys, Set<V> required) {
        Batch {
            keys = List.copyOf(keys);
            required = Set.copyOf(required);
        }
    }

    static <K, V> List<Batch<K, V>> plan(Collection<Unit<K, V>> units, int maxActiveValues) {
        if(maxActiveValues <= 0)
            throw new IllegalArgumentException("maxActiveValues must be positive");

        var batches = new ArrayList<Batch<K, V>>();
        var keys = new ArrayList<K>();
        var required = new LinkedHashSet<V>();
        for(var unit : units) {
            if(unit.required().size() > maxActiveValues) {
                throw new IllegalArgumentException(
                        "A rebuild unit requires " + unit.required().size()
                                + " values, above the active limit of " + maxActiveValues
                );
            }

            var combined = new LinkedHashSet<>(required);
            combined.addAll(unit.required());
            if(!keys.isEmpty() && combined.size() > maxActiveValues) {
                batches.add(new Batch<>(keys, required));
                keys = new ArrayList<>();
                required = new LinkedHashSet<>();
            }

            keys.add(unit.key());
            required.addAll(unit.required());
        }
        if(!keys.isEmpty())
            batches.add(new Batch<>(keys, required));
        return List.copyOf(batches);
    }
}
