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
package org.patryk3211.powergrid.commands;

import java.util.Map;
import java.util.WeakHashMap;

final class RebuildCooldown<K> {
    private final Map<K, Long> nextAllowedTicks = new WeakHashMap<>();
    private final long cooldownTicks;

    RebuildCooldown(long cooldownTicks) {
        if(cooldownTicks <= 0)
            throw new IllegalArgumentException("Cooldown must be positive");
        this.cooldownTicks = cooldownTicks;
    }

    synchronized boolean tryAcquire(K key, long currentTick) {
        if(remainingTicks(key, currentTick) > 0)
            return false;
        nextAllowedTicks.put(key, currentTick + cooldownTicks);
        return true;
    }

    synchronized long remainingTicks(K key, long currentTick) {
        var nextAllowedTick = nextAllowedTicks.get(key);
        if(nextAllowedTick == null)
            return 0;
        return Math.max(0, nextAllowedTick - currentTick);
    }
}
