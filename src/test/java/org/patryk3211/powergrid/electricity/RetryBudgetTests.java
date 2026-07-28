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

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.SplitCordEndpoint;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RetryBudgetTests {
    @Test
    void automaticRetryBudgetStopsAfterThreeAttempts() {
        var budget = new RetryBudget(WorldNetworks.MAX_AUTOMATIC_REBUILD_RETRIES);

        Assertions.assertTrue(budget.tryAcquire());
        Assertions.assertTrue(budget.tryAcquire());
        Assertions.assertTrue(budget.tryAcquire());
        Assertions.assertFalse(budget.tryAcquire());

        Assertions.assertTrue(budget.exhausted());
        Assertions.assertEquals(3, budget.attempts());
        Assertions.assertEquals(0, budget.remaining());
    }

    @Test
    void automaticRetryBudgetRejectsInvalidLimits() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RetryBudget(0));
    }

    @Test
    void physicalRegistrationRefreshHasThreeTotalAttempts() {
        var budget = new RetryBudget(
                TransmissionNetworkRebuildJob.MAX_PHYSICAL_REGISTRATION_ATTEMPTS
        );

        Assertions.assertTrue(budget.tryAcquire());
        Assertions.assertTrue(budget.tryAcquire());
        Assertions.assertTrue(budget.tryAcquire());
        Assertions.assertFalse(budget.tryAcquire());
        Assertions.assertEquals(3, budget.attempts());
    }

    @Test
    void physicalRegistrationRefreshWaitsForBothValidEndpoints() {
        Assertions.assertTrue(TransmissionNetworkRebuildJob.registrationReady(
                true, true, true, true, true
        ));
        Assertions.assertFalse(TransmissionNetworkRebuildJob.registrationReady(
                true, true, true, true, false
        ));
        Assertions.assertFalse(TransmissionNetworkRebuildJob.registrationReady(
                true, false, false, true, true
        ));
        Assertions.assertFalse(TransmissionNetworkRebuildJob.registrationReady(
                false, true, true, true, true
        ));
    }

    @Test
    void startupCanRetainMatchingPhysicalRegistrationBeforeRuntimeBehaviourIsReady() {
        Assertions.assertTrue(TransmissionNetworkRebuildJob.canRetainPersistedRegistration(
                true, true, true, true, true
        ));

        Assertions.assertFalse(TransmissionNetworkRebuildJob.canRetainPersistedRegistration(
                false, true, true, true, true
        ));
        Assertions.assertFalse(TransmissionNetworkRebuildJob.canRetainPersistedRegistration(
                true, false, true, true, true
        ));
        Assertions.assertFalse(TransmissionNetworkRebuildJob.canRetainPersistedRegistration(
                true, true, false, true, true
        ));
        Assertions.assertFalse(TransmissionNetworkRebuildJob.canRetainPersistedRegistration(
                true, true, true, false, true
        ));
        Assertions.assertFalse(TransmissionNetworkRebuildJob.canRetainPersistedRegistration(
                true, true, true, true, false
        ));
    }

    @Test
    void cordRegistrationUsesThePhysicalEndpointForItsConductor() {
        var endpoint0 = new BlockWireEndpoint(new BlockPos(1, 2, 3), 0);
        var endpoint1 = new BlockWireEndpoint(new BlockPos(1, 2, 3), 1);
        var cordEndpoint = new SplitCordEndpoint(endpoint0, endpoint1);
        var ownerId = UUID.randomUUID();

        Assertions.assertSame(
                endpoint0,
                TransmissionNetworkRebuildJob.connectionEndpointForPart(
                        cordEndpoint,
                        new WorldNetworks.ComplexId(ownerId, 0)
                )
        );
        Assertions.assertSame(
                endpoint1,
                TransmissionNetworkRebuildJob.connectionEndpointForPart(
                        cordEndpoint,
                        new WorldNetworks.ComplexId(ownerId, 1)
                )
        );
        Assertions.assertNull(
                TransmissionNetworkRebuildJob.connectionEndpointForPart(
                        cordEndpoint,
                        new WorldNetworks.ComplexId(ownerId, 2)
                )
        );
    }

    @Test
    void postIsolationSettlementReconcilesOnlyRegistrationsThatReturned() {
        var unresolved = new HashSet<>(Set.of("ready-a", "missing", "ready-b"));
        var verified = new HashSet<>(Set.of("already-verified"));

        var recovered = TransmissionNetworkRebuildJob.reconcileRecoveredRegistrations(
                unresolved,
                verified,
                id -> id.startsWith("ready-")
        );

        Assertions.assertEquals(2, recovered);
        Assertions.assertEquals(Set.of("missing"), unresolved);
        Assertions.assertEquals(
                Set.of("already-verified", "ready-a", "ready-b"),
                verified
        );
    }
}
