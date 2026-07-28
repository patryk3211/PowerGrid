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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RebuildCooldownTests {
    @Test
    void cooldownIsIndependentPerDimensionAndExpires() {
        var limiter = new RebuildCooldown<Object>(1200);
        var overworld = new Object();
        var nether = new Object();

        Assertions.assertTrue(limiter.tryAcquire(overworld, 100));
        Assertions.assertFalse(limiter.tryAcquire(overworld, 100));
        Assertions.assertEquals(1200, limiter.remainingTicks(overworld, 100));

        Assertions.assertTrue(limiter.tryAcquire(nether, 100));
        Assertions.assertEquals(1, limiter.remainingTicks(overworld, 1299));
        Assertions.assertTrue(limiter.tryAcquire(overworld, 1300));
    }
}
