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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class RebuildBatchPlannerTests {
    @Test
    void ownerAndEndpointChunksStayInTheSameBoundedBatch() {
        var units = List.of(
                new RebuildBatchPlanner.Unit<>("part-a", Set.of(1, 2)),
                new RebuildBatchPlanner.Unit<>("part-b", Set.of(2, 3)),
                new RebuildBatchPlanner.Unit<>("part-c", Set.of(3, 4))
        );

        var batches = RebuildBatchPlanner.plan(units, 3);

        Assertions.assertEquals(2, batches.size());
        Assertions.assertEquals(List.of("part-a", "part-b"), batches.get(0).keys());
        Assertions.assertEquals(Set.of(1, 2, 3), batches.get(0).required());
        Assertions.assertEquals(List.of("part-c"), batches.get(1).keys());
        Assertions.assertEquals(Set.of(3, 4), batches.get(1).required());
        Assertions.assertTrue(batches.stream().allMatch(batch -> batch.required().size() <= 3));
    }

    @Test
    void aSingleOversizedPhysicalPartIsRejected() {
        var units = List.of(
                new RebuildBatchPlanner.Unit<>("part-a", Set.of(1, 2, 3))
        );

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RebuildBatchPlanner.plan(units, 2)
        );
    }
}
