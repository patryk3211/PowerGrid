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
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.JunctionWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.ICordEndpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Performs a verified rebuild without keeping the whole electrical network
 * loaded at once. Each physical part is scanned with its owner and endpoint
 * chunks active together, then all temporary tickets are released before the
 * refreshed logical topology is assembled.
 */
public final class TransmissionNetworkRebuildJob {
    static final int MAX_ACTIVE_CHUNKS = 8;
    public static final int MAX_UNIQUE_CHUNKS = 512;
    static final int MAX_CHUNK_LOAD_ATTEMPTS = 3;
    static final int CHUNK_LOAD_ATTEMPT_TIMEOUT_TICKS = 100;
    static final int CHUNK_LOAD_RETRY_DELAY_TICKS = 20;
    static final int MAX_PHYSICAL_REGISTRATION_ATTEMPTS = 3;
    static final int PHYSICAL_REGISTRATION_RETRY_DELAY_TICKS = 20;
    static final int POST_ISOLATION_REGISTRATION_GRACE_TICKS = 20 * 5;
    static final int ENTITY_REFRESH_GRACE_TICKS = WorldNetworks.ENTITY_LOAD_GRACE_TICKS + 2;
    static final int MAX_JOB_DURATION_TICKS = 20 * 60 * 5;
    private static final int TICKET_TIMEOUT_TICKS = CHUNK_LOAD_ATTEMPT_TIMEOUT_TICKS * 2;
    private static final int TICKET_DISTANCE = 2;
    private static final TicketType<ChunkPos> REBUILD_TICKET = TicketType.create(
            "powergrid_verified_rebuild",
            Comparator.comparingLong(ChunkPos::toLong),
            TICKET_TIMEOUT_TICKS
    );

    public interface Listener {
        void completed(Outcome outcome);

        void failed(Failure failure);
    }

    public record StartInfo(int parts, int uniqueChunks, int batches, int maxUniqueChunks) {
    }

    public record Outcome(
            WorldNetworks.RebuildResult rebuild,
            int uniqueChunks,
            int batches,
            int verifiedParts,
            int unresolvedParts,
            int refreshedRecords,
            int recoveredRecords,
            int removedRecords,
            long elapsedMillis
    ) {
        public boolean complete() {
            return unresolvedParts == 0 && rebuild.complete();
        }
    }

    public record Failure(String reason, Collection<ChunkPos> chunks, int attempts, long elapsedMillis) {
        public Failure {
            chunks = List.copyOf(chunks);
        }
    }

    static final class TooManyChunksException extends IllegalArgumentException {
        private final int chunks;

        TooManyChunksException(int chunks) {
            super("Verified rebuild needs " + chunks + " unique chunks, above the limit of " + MAX_UNIQUE_CHUNKS);
            this.chunks = chunks;
        }

        int chunks() {
            return chunks;
        }
    }

    private record PartSnapshot(
            TransmissionLinePart instance,
            IWireEndpoint endpoint1,
            IWireEndpoint endpoint2
    ) {
    }

    private record CurrentPartEndpoints(
            IWireEndpoint endpoint1,
            IWireEndpoint endpoint2
    ) {
    }

    private record Plan(
            Map<WorldNetworks.PartId, PartSnapshot> initialParts,
            List<RebuildBatchPlanner.Batch<WorldNetworks.PartId, ChunkPos>> batches,
            int uniqueChunks
    ) {
    }

    private final WorldNetworks networks;
    private final ServerLevel world;
    private final Listener listener;
    private final Plan plan;
    private final long startedAtTick;
    private final long startedAtNanos;
    private final Set<WorldNetworks.PartId> verifiedParts = new HashSet<>();
    private final Set<WorldNetworks.PartId> unresolvedParts = new HashSet<>();
    private final Set<WorldNetworks.PartId> retainedRegistrations = new HashSet<>();
    private final Set<ChunkPos> activeTickets = new LinkedHashSet<>();

    private int batchIndex = -1;
    private RebuildBatchPlanner.Batch<WorldNetworks.PartId, ChunkPos> currentBatch;
    private RetryBudget currentBatchBudget;
    private RetryBudget currentRegistrationBudget;
    private Set<WorldNetworks.PartId> pendingRegistrationIds = Set.of();
    private long attemptStartedAt;
    private long retryAt;
    private long readySince = -1;
    private long postIsolationRegistrationDeadline = -1;
    private WorldNetworks.RebuildResult completedIsolationRebuild;
    private boolean refreshRequested;
    private boolean finished;

    static TransmissionNetworkRebuildJob create(
            WorldNetworks networks,
            ServerLevel world,
            Listener listener
    ) {
        return new TransmissionNetworkRebuildJob(networks, world, listener, createPlan(networks, world));
    }

    private TransmissionNetworkRebuildJob(
            WorldNetworks networks,
            ServerLevel world,
            Listener listener,
            Plan plan
    ) {
        this.networks = networks;
        this.world = world;
        this.listener = listener;
        this.plan = plan;
        this.startedAtTick = world.getGameTime();
        this.startedAtNanos = System.nanoTime();
    }

    StartInfo startInfo() {
        return new StartInfo(
                plan.initialParts().size(),
                plan.uniqueChunks(),
                plan.batches().size(),
                MAX_UNIQUE_CHUNKS
        );
    }

    boolean isFinished() {
        return finished;
    }

    void tick() {
        if(finished)
            return;

        try {
            var currentTick = world.getGameTime();
            if(currentTick - startedAtTick > MAX_JOB_DURATION_TICKS) {
                fail("maximum rebuild duration exceeded", activeTickets, attempts());
                return;
            }

            if(completedIsolationRebuild != null) {
                finishPostIsolationRegistrationCheck(currentTick);
                return;
            }

            if(currentBatch == null) {
                if(batchIndex + 1 >= plan.batches().size()) {
                    finish(currentTick);
                    return;
                }
                beginNextBatch(currentTick);
                return;
            }

            if(currentTick < retryAt)
                return;

            if(activeTickets.isEmpty()) {
                beginAttempt(currentTick);
                return;
            }

            if(allChunksReady()) {
                if(!refreshRequested) {
                    if(!currentRegistrationBudget.tryAcquire()) {
                        finishOrScheduleRegistrationRetry(currentTick);
                        return;
                    }
                    refreshPhysicalOwnerRegistrations(pendingRegistrationIds);
                    refreshRequested = true;
                    readySince = currentTick;
                    return;
                }
                if(currentTick - readySince < ENTITY_REFRESH_GRACE_TICKS)
                    return;

                finishOrScheduleRegistrationRetry(currentTick);
                return;
            }

            readySince = -1;
            refreshRequested = false;
            if(currentTick - attemptStartedAt < CHUNK_LOAD_ATTEMPT_TIMEOUT_TICKS)
                return;

            var exhausted = currentBatchBudget.exhausted();
            releaseActiveTickets();
            if(exhausted) {
                fail(
                        "chunk batch did not reach entity-ticking state",
                        currentBatch.required(),
                        currentBatchBudget.attempts()
                );
            } else {
                retryAt = currentTick + CHUNK_LOAD_RETRY_DELAY_TICKS;
            }
        } catch(RuntimeException exception) {
            PowerGrid.LOGGER.error(
                    "Verified transmission rebuild crashed in {}",
                    world.dimension().location(),
                    exception
            );
            fail("unexpected rebuild exception: " + exception.getClass().getSimpleName(), activeTickets, attempts());
        }
    }

    void cancel() {
        releaseActiveTickets();
        networks.abortVerifiedRebuildIsolation();
        finished = true;
    }

    private void beginNextBatch(long currentTick) {
        currentBatch = plan.batches().get(++batchIndex);
        currentBatchBudget = new RetryBudget(MAX_CHUNK_LOAD_ATTEMPTS);
        currentRegistrationBudget = new RetryBudget(MAX_PHYSICAL_REGISTRATION_ATTEMPTS);
        pendingRegistrationIds = Set.copyOf(currentBatch.keys());
        retryAt = currentTick;
        beginAttempt(currentTick);
    }

    private void beginAttempt(long currentTick) {
        if(!currentBatchBudget.tryAcquire()) {
            fail("chunk retry budget exhausted", currentBatch.required(), currentBatchBudget.attempts());
            return;
        }

        attemptStartedAt = currentTick;
        readySince = -1;
        refreshRequested = false;
        for(var chunk : currentBatch.required()) {
            world.getChunkSource().addRegionTicket(
                    REBUILD_TICKET,
                    chunk,
                    TICKET_DISTANCE,
                    chunk
            );
            activeTickets.add(chunk);
        }
        PowerGrid.LOGGER.info(
                "Verified transmission rebuild in {} loading batch {}/{}: {} chunks, attempt {}/{}",
                world.dimension().location(),
                batchIndex + 1,
                plan.batches().size(),
                activeTickets.size(),
                currentBatchBudget.attempts(),
                MAX_CHUNK_LOAD_ATTEMPTS
        );
    }

    private boolean allChunksReady() {
        for(var chunkPos : activeTickets) {
            var chunk = world.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
            if(chunk == null
                    || chunk.getFullStatus() != FullChunkStatus.ENTITY_TICKING
                    || !world.areEntitiesLoaded(chunkPos.toLong())) {
                return false;
            }
        }
        return true;
    }

    private void refreshPhysicalOwnerRegistrations(Collection<WorldNetworks.PartId> ids) {
        var refreshedEntities = new HashMap<java.util.UUID, BaseWireEntity>();
        var readyIds = new HashSet<WorldNetworks.PartId>();
        for(var id : ids) {
            var owner = id.getEntity(world);
            if(owner != null && physicalOwnerReady(owner)) {
                refreshedEntities.putIfAbsent(owner.getUUID(), owner);
                readyIds.add(id);
            }
        }

        // Do not remove a persisted registration while the physical entity's
        // endpoints are still attaching their block behaviours. Startup can
        // load a wire entity before one of those late behaviours exists.
        // Ready owners remain authoritative and are refreshed normally.
        for(var owner : refreshedEntities.values())
            owner.dropWire();
        for(var id : readyIds) {
            var owner = id.getEntity(world);
            if(owner != null)
                networks.removePartForVerifiedRefresh(id, owner);
        }
        for(var owner : refreshedEntities.values())
            owner.makeWire();
    }

    private boolean physicalOwnerReady(BaseWireEntity owner) {
        var endpoint1 = owner.getEndpoint1();
        var endpoint2 = owner.getEndpoint2();
        var endpoint1Valid = endpoint1 != null && endpoint1.isValid(world);
        var endpoint2Valid = endpoint2 != null && endpoint2.isValid(world);
        return registrationReady(
                !owner.isRemoved(),
                endpoint1 != null,
                endpoint1Valid,
                endpoint2 != null,
                endpoint2Valid
        );
    }

    static boolean registrationReady(
            boolean ownerLoaded,
            boolean endpoint1Present,
            boolean endpoint1Valid,
            boolean endpoint2Present,
            boolean endpoint2Valid
    ) {
        return ownerLoaded
                && endpoint1Present
                && endpoint1Valid
                && endpoint2Present
                && endpoint2Valid;
    }

    static boolean canRetainPersistedRegistration(
            boolean ownerLoaded,
            boolean partPresent,
            boolean endpointsMatch,
            boolean endpoint1StructurallyValid,
            boolean endpoint2StructurallyValid
    ) {
        return ownerLoaded
                && partPresent
                && endpointsMatch
                && endpoint1StructurallyValid
                && endpoint2StructurallyValid;
    }

    private Set<WorldNetworks.PartId> verifyCurrentBatch() {
        var attemptUnresolved = new HashSet<WorldNetworks.PartId>();
        for(var id : currentBatch.keys()) {
            var owner = id.getEntity(world);
            var part = networks.getPart(id);
            if(owner != null && part != null && part.owner == owner) {
                verifiedParts.add(id);
                unresolvedParts.remove(id);
            } else if(owner == null) {
                networks.removeStalePartAfterVerifiedScan(id);
                unresolvedParts.remove(id);
            } else if(retainPersistedRegistration(id, owner, part)) {
                verifiedParts.add(id);
                unresolvedParts.remove(id);
            } else {
                unresolvedParts.add(id);
                attemptUnresolved.add(id);
            }
        }
        return Set.copyOf(attemptUnresolved);
    }

    private boolean retainPersistedRegistration(
            WorldNetworks.PartId id,
            BaseWireEntity owner,
            TransmissionLinePart part
    ) {
        var currentEndpoints = currentPartEndpoints(owner, id);
        var endpoint1 = currentEndpoints.endpoint1();
        var endpoint2 = currentEndpoints.endpoint2();
        var endpointsMatch = part != null
                && endpoint1 != null
                && endpoint2 != null
                && part.endpointsMatch(endpoint1, endpoint2);
        var endpoint1StructurallyValid =
                endpoint1 != null && endpoint1.isStructurallyValid(world);
        var endpoint2StructurallyValid =
                endpoint2 != null && endpoint2.isStructurallyValid(world);
        if(!canRetainPersistedRegistration(
                !owner.isRemoved(),
                part != null,
                endpointsMatch,
                endpoint1StructurallyValid,
                endpoint2StructurallyValid
        )) {
            return false;
        }
        if(!part.grab(owner, id))
            return false;

        if(retainedRegistrations.add(id)) {
            PowerGrid.LOGGER.info(
                    "Verified transmission rebuild in {} retained persisted registration {} after physical endpoint validation: ({}, {}), runtimeEndpoint1Ready={}, runtimeEndpoint2Ready={}",
                    world.dimension().location(),
                    id,
                    endpoint1,
                    endpoint2,
                    endpoint1.isValid(world),
                    endpoint2.isValid(world)
            );
        }
        return true;
    }

    private static CurrentPartEndpoints currentPartEndpoints(
            BaseWireEntity owner,
            WorldNetworks.PartId id
    ) {
        return new CurrentPartEndpoints(
                connectionEndpointForPart(owner.getEndpoint1(), id),
                connectionEndpointForPart(owner.getEndpoint2(), id)
        );
    }

    static IWireEndpoint connectionEndpointForPart(
            IWireEndpoint ownerEndpoint,
            WorldNetworks.PartId id
    ) {
        if(id instanceof WorldNetworks.ComplexId complexId) {
            if(!(ownerEndpoint instanceof ICordEndpoint cordEndpoint))
                return null;
            return switch(complexId.sub()) {
                case 0 -> cordEndpoint.getEndpoint1();
                case 1 -> cordEndpoint.getEndpoint2();
                default -> null;
            };
        }
        return ownerEndpoint;
    }

    private void finishOrScheduleRegistrationRetry(long currentTick) {
        var attemptUnresolved = verifyCurrentBatch();
        if(!attemptUnresolved.isEmpty() && !currentRegistrationBudget.exhausted()) {
            pendingRegistrationIds = attemptUnresolved;
            refreshRequested = false;
            readySince = -1;
            retryAt = currentTick + PHYSICAL_REGISTRATION_RETRY_DELAY_TICKS;
            PowerGrid.LOGGER.warn(
                    "Verified transmission rebuild in {} will retry {} unresolved physical registrations from batch {}/{}; next attempt {}/{}",
                    world.dimension().location(),
                    attemptUnresolved.size(),
                    batchIndex + 1,
                    plan.batches().size(),
                    currentRegistrationBudget.attempts() + 1,
                    MAX_PHYSICAL_REGISTRATION_ATTEMPTS
            );
            return;
        }

        if(!attemptUnresolved.isEmpty()) {
            logExhaustedPhysicalRegistrations(attemptUnresolved);
            removeUnverifiedPhysicalRegistrations(attemptUnresolved);
        }
        releaseActiveTickets();
        currentBatch = null;
        pendingRegistrationIds = Set.of();
    }

    private void removeUnverifiedPhysicalRegistrations(Set<WorldNetworks.PartId> ids) {
        for(var id : ids) {
            var owner = id.getEntity(world);
            if(owner != null)
                networks.removePartForVerifiedRefresh(id, owner);
        }
    }

    private void logExhaustedPhysicalRegistrations(Set<WorldNetworks.PartId> ids) {
        for(var id : ids) {
            var owner = id.getEntity(world);
            var initial = plan.initialParts().get(id);
            var currentEndpoints = owner != null
                    ? currentPartEndpoints(owner, id)
                    : new CurrentPartEndpoints(null, null);
            var currentEndpoint1 = currentEndpoints.endpoint1();
            var currentEndpoint2 = currentEndpoints.endpoint2();
            var currentEndpoint1Valid = currentEndpoint1 != null && currentEndpoint1.isValid(world);
            var currentEndpoint2Valid = currentEndpoint2 != null && currentEndpoint2.isValid(world);
            var currentEndpoint1StructurallyValid =
                    currentEndpoint1 != null && currentEndpoint1.isStructurallyValid(world);
            var currentEndpoint2StructurallyValid =
                    currentEndpoint2 != null && currentEndpoint2.isStructurallyValid(world);
            PowerGrid.LOGGER.warn(
                    "Physical transmission registration {} remains unresolved after {}/{} attempts in {} "
                            + "(ownerLoaded={}, ownerType={}, ownerPos={}, currentEndpoint1={}, currentEndpoint1Valid={}, currentEndpoint1StructurallyValid={}, "
                            + "currentEndpoint2={}, currentEndpoint2Valid={}, currentEndpoint2StructurallyValid={}, savedEndpoint1={}, savedEndpoint2={})",
                    id,
                    currentRegistrationBudget.attempts(),
                    MAX_PHYSICAL_REGISTRATION_ATTEMPTS,
                    world.dimension().location(),
                    owner != null && !owner.isRemoved(),
                    owner != null ? owner.getType() : null,
                    owner != null ? owner.blockPosition() : null,
                    currentEndpoint1,
                    currentEndpoint1Valid,
                    currentEndpoint1StructurallyValid,
                    currentEndpoint2,
                    currentEndpoint2Valid,
                    currentEndpoint2StructurallyValid,
                    initial != null ? initial.endpoint1() : null,
                    initial != null ? initial.endpoint2() : null
            );
        }
    }

    private void finish(long currentTick) {
        releaseActiveTickets();
        completedIsolationRebuild = networks.completeVerifiedRebuildIsolation();
        if(!unresolvedParts.isEmpty()) {
            postIsolationRegistrationDeadline =
                    currentTick + POST_ISOLATION_REGISTRATION_GRACE_TICKS;
            PowerGrid.LOGGER.info(
                    "Verified transmission rebuild in {} opened a {}-tick post-isolation settlement window for {} late physical registrations; no additional rebuild attempts will be made",
                    world.dimension().location(),
                    POST_ISOLATION_REGISTRATION_GRACE_TICKS,
                    unresolvedParts.size()
            );
            return;
        }
        completeOutcome(completedIsolationRebuild);
    }

    private void finishPostIsolationRegistrationCheck(long currentTick) {
        var recovered = reconcileRecoveredRegistrations(
                unresolvedParts,
                verifiedParts,
                id -> {
                    var owner = id.getEntity(world);
                    var part = networks.getPart(id);
                    return part != null && (owner == null || part.owner == owner);
                }
        );
        if(recovered > 0) {
            PowerGrid.LOGGER.info(
                    "Verified transmission rebuild in {} observed {} late physical registrations after topology isolation ended; {} remain unresolved",
                    world.dimension().location(),
                    recovered,
                    unresolvedParts.size()
            );
        }
        if(unresolvedParts.isEmpty() || currentTick >= postIsolationRegistrationDeadline)
            completeOutcome(networks.refreshVerifiedRebuildResult(completedIsolationRebuild));
    }

    static <T> int reconcileRecoveredRegistrations(
            Set<T> unresolved,
            Set<T> verified,
            Predicate<T> registered
    ) {
        int recovered = 0;
        var iterator = unresolved.iterator();
        while(iterator.hasNext()) {
            var id = iterator.next();
            if(!registered.test(id))
                continue;
            iterator.remove();
            verified.add(id);
            ++recovered;
        }
        return recovered;
    }

    private void completeOutcome(WorldNetworks.RebuildResult rebuild) {
        var currentParts = networks.linePartsSnapshot();
        int refreshedRecords = 0;
        int removedRecords = 0;
        for(var entry : plan.initialParts().entrySet()) {
            var current = currentParts.get(entry.getKey());
            if(current == null) {
                ++removedRecords;
                continue;
            }
            var initial = entry.getValue();
            if(current != initial.instance()
                    || !TransmissionLinePart.sameEndpoints(
                            initial.endpoint1(),
                            initial.endpoint2(),
                            current.getConnectionEndpoint1(),
                            current.getConnectionEndpoint2()
                    )) {
                ++refreshedRecords;
            }
        }
        int recoveredRecords = 0;
        for(var id : currentParts.keySet()) {
            if(!plan.initialParts().containsKey(id))
                ++recoveredRecords;
        }

        var elapsedMillis = elapsedMillis();
        var outcome = new Outcome(
                rebuild,
                plan.uniqueChunks(),
                plan.batches().size(),
                verifiedParts.size(),
                unresolvedParts.size(),
                refreshedRecords,
                recoveredRecords,
                removedRecords,
                elapsedMillis
        );
        PowerGrid.LOGGER.info(
                "Verified transmission rebuild completed in {}: {} unique chunks, {} batches, {} parts verified, {} unresolved parts, {} persisted registrations retained after structural validation, {} records refreshed, {} recovered, {} removed, {} ms",
                world.dimension().location(),
                outcome.uniqueChunks(),
                outcome.batches(),
                outcome.verifiedParts(),
                outcome.unresolvedParts(),
                retainedRegistrations.size(),
                outcome.refreshedRecords(),
                outcome.recoveredRecords(),
                outcome.removedRecords(),
                outcome.elapsedMillis()
        );
        finished = true;
        listener.completed(outcome);
    }

    private void fail(String reason, Collection<ChunkPos> chunks, int attempts) {
        if(finished)
            return;
        var failedChunks = List.copyOf(chunks);
        releaseActiveTickets();
        networks.abortVerifiedRebuildIsolation();
        var failure = new Failure(reason, failedChunks, attempts, elapsedMillis());
        PowerGrid.LOGGER.error(
                "Verified transmission rebuild failed in {} after {} attempts and {} ms: {}; chunks={}",
                world.dimension().location(),
                attempts,
                failure.elapsedMillis(),
                reason,
                failure.chunks()
        );
        finished = true;
        listener.failed(failure);
    }

    private int attempts() {
        return currentBatchBudget == null ? 0 : currentBatchBudget.attempts();
    }

    private long elapsedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }

    private void releaseActiveTickets() {
        for(var chunk : activeTickets) {
            world.getChunkSource().removeRegionTicket(
                    REBUILD_TICKET,
                    chunk,
                    TICKET_DISTANCE,
                    chunk
            );
        }
        activeTickets.clear();
    }

    private static Plan createPlan(WorldNetworks networks, ServerLevel world) {
        var initial = networks.linePartsSnapshot();
        var ordered = new ArrayList<>(initial.entrySet());
        ordered.sort(Comparator.comparing(entry -> entry.getKey().toString()));

        var snapshots = new HashMap<WorldNetworks.PartId, PartSnapshot>();
        var units = new ArrayList<RebuildBatchPlanner.Unit<WorldNetworks.PartId, ChunkPos>>();
        var uniqueChunks = new LinkedHashSet<ChunkPos>();
        for(var entry : ordered) {
            var id = entry.getKey();
            var part = entry.getValue();
            snapshots.put(
                    id,
                    new PartSnapshot(
                            part,
                            part.getConnectionEndpoint1(),
                            part.getConnectionEndpoint2()
                    )
            );

            var required = requiredChunks(part, world);
            units.add(new RebuildBatchPlanner.Unit<>(id, required));
            uniqueChunks.addAll(required);
        }

        if(uniqueChunks.size() > MAX_UNIQUE_CHUNKS)
            throw new TooManyChunksException(uniqueChunks.size());

        var batches = RebuildBatchPlanner.plan(units, MAX_ACTIVE_CHUNKS);
        return new Plan(Map.copyOf(snapshots), batches, uniqueChunks.size());
    }

    private static Set<ChunkPos> requiredChunks(TransmissionLinePart part, ServerLevel world) {
        var required = new LinkedHashSet<ChunkPos>();
        required.add(part.lastKnownChunk);
        if(part.owner != null && !part.owner.isRemoved())
            required.add(new ChunkPos(part.owner.blockPosition()));
        addEndpointChunk(required, part.getConnectionEndpoint1(), world);
        addEndpointChunk(required, part.getConnectionEndpoint2(), world);
        return Set.copyOf(required);
    }

    private static void addEndpointChunk(Set<ChunkPos> chunks, IWireEndpoint endpoint, ServerLevel world) {
        if(endpoint instanceof BlockWireEndpoint block) {
            chunks.add(new ChunkPos(block.getPos()));
        } else if(endpoint instanceof JunctionWireEndpoint junction) {
            chunks.add(new ChunkPos(BlockPos.containing(junction.getExactPosition(world))));
        }
    }
}
