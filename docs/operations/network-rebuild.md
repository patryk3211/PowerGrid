---
title: Transmission network rebuild
---

# Transmission network rebuild

The server performs one verified transmission rebuild for every dimension
during each startup. It also adds:

```text
/powergrid rebuild
```

The command requires Minecraft permission level 2 (operator). It affects only
the command source's current dimension; a dedicated-server console invocation
uses its current command level, normally the overworld. Existing Power Grid
debug, source, performance and configuration commands keep their level 3
permission requirement.

Only one verified rebuild can run in a dimension at a time. If the automatic
startup rebuild is still active, the command reports that a rebuild is already
running. An accepted manual invocation has a per-dimension 60-second cooldown
to limit accidental or malicious repeated work.

## What the command does

`/powergrid rebuild` starts the same asynchronous state machine used during
startup. The command returns after scheduling the job; the server thread
advances one bounded phase per tick and reports the final result to the command
source.

For the current dimension, the command:

1. Uses saved transmission-part registrations to discover only chunks which
   are known to contain a wire owner or one of its endpoints.
2. Refuses the operation before isolation when the plan exceeds the
   512-unique-chunk safety limit.
3. Temporarily pauses the dimension's derived transmission topology, island
   discovery, electrical solver work and state synchronization. Physical block
   and wire entities continue their normal server ticks.
4. Adds entity-ticking tickets for at most eight chunks at a time, verifies the
   physical owners and their terminal structures, refreshes stale
   registrations, and releases each batch before loading the next one.
5. Recreates the endpoint indexes and optimized transmission lines once, after
   the verified scan has finished, then resumes normal electrical simulation.
6. Reports the physical-part count, unique chunks, refreshed, recovered and
   removed records, final line count and elapsed time. A partial or failed job
   directs the operator to the diagnostic server log.

The command does not break or replace placed blocks, does not scan every
generated chunk, and does not keep network chunks permanently loaded. All
temporary tickets are released after a batch, on completion, on failure,
dimension unload and server shutdown; the ticket type also has an expiry.

## Verified rebuild phases

The rebuild treats physical wire and cord entities as the source of truth:

1. Read the persisted physical-part registrations and collect each part's last
   known owner chunk and endpoint chunks.
2. Detach the derived transmission topology and pause transmission-line island
   discovery and solver work. Physical entities continue ticking, but no
   partially refreshed topology can enter the solver.
3. Load owner and endpoint chunks together in bounded groups, wait until
   entities are loaded and ticking, and refresh the physical owner's electrical
   registrations from its current endpoints while they remain detached from the
   solver graph. A present owner is refreshed only after both of its current
   endpoints exist and validate against their loaded electrical behaviours.
4. If a loaded physical owner reports the same endpoint pair as its saved
   registration, but a block such as the generator commutator has not created
   its late electrical behaviour yet, retain and reattach that saved
   registration after verifying that both physical terminal structures still
   exist. The final topology uses placeholder nodes until the normal block
   lifecycle supplies the live nodes.
5. Replace only the stale logical part when its saved endpoints differ from
   the physical entity. The repair never removes an entire optimized line just
   because one segment is stale.
6. Remove a saved registration only after its complete owner/endpoint group was
   entity-ticking and no physical owner existed.
7. Release that group's temporary chunk tickets before processing the next
   group.
8. Restore the endpoint and node indexes, assemble fresh optimized logical
   lines once from the refreshed registrations, and only then resume island
   discovery and solver work.
9. If a present owner was still waiting for late block-behaviour initialization
   after its third bounded refresh attempt, observe normal registration for at
   most 100 additional ticks after isolation ends. This settlement phase makes
   no fourth rebuild attempt and acquires no chunk ticket. It only accepts a
   registration that the physical owner recreates through its normal tick or
   initialization lifecycle.

The verified startup rebuild does not depend on a player being logged in or
near the network. Server-owned entity-ticking tickets drive the bounded scan.
Matching persisted registrations can be retained from the physical blocks and
wire entities before a late runtime electrical behaviour becomes available, so
player login, movement and manual rebuilds are not initialization triggers.

Electrical block initialization during the verified scan may update endpoint
and node indexes, but it cannot start tree tracing, line creation or line
splitting. Those topology operations are enabled only for the single final
assembly phase.

Placed blocks, physical wire entities and cords are not removed. The rebuild
temporarily interrupts transmission while the verified scan is active.

After the verified scan, passive transmission topology uses placeholder nodes
for endpoints whose chunks unload again. The network therefore does not keep
its chunks permanently loaded.

Unloading a terminal does not detach a healthy derived transmission line.
Instead, it atomically rebinds the existing line, every endpoint alias and every
persisted physical segment from the departing block node to a placeholder node.
The node-to-part index never keeps the discarded block node as its only key,
and the optimized line remains connected to the same solver network and global
graph. When the endpoint loads again, normal node registration migrates the
line and indexed segment set from the placeholder to the real node. Island
discovery is scheduled after both transitions.

This unload/reload path does not consume the automatic rebuild retry budget and
does not acquire chunk tickets. A manual `/powergrid rebuild` may still be
needed once after installing the repair to correct a world whose runtime index
was already orphaned by an older build. It must not be required again merely
because a player leaves and revisits the repaired endpoint chunks.

Preserving the derived topology means the server retains the compact logical
lines assembled by the verified rebuild even when their physical chunks are
absent. It does not retain wire entities, block entities or chunks. The solver
and graph memory cost therefore follows the persisted electrical network size,
not the number of loaded terrain chunks.

When an endpoint chunk loads again, its real node supersedes the persisted
placeholder and all indexed transmission parts migrate to that real node.
Multiblock proxy endpoints are valid aliases: an endpoint on a secondary block
may resolve to a node canonically owned by the main block, so their endpoint
objects do not have to be equal. Each persisted segment therefore retains its
physical connection endpoints separately from its resolved canonical nodes.
The physical pair is used for owner validation, chunk planning and saved data;
the canonical nodes are used for topology traversal and solver connections.
Tree traversal selects a segment side by canonical node identity rather than by
comparing the physical alias with the canonical endpoint.

Ordinary endpoint registration binds and migrates the selected node without
requesting a topology split. Callers which genuinely introduce a proxy alias
request the split explicitly after binding. This separation prevents
`TransmissionLine.splitAt` from re-entering itself when its connection
preparation registers the same boundary endpoints.

The existing `Node1` and `Node2` saved-data keys now retain the physical
connection pair. No world-data migration or new format version is required.
The first verified rebuild after upgrading from an earlier server fix refreshes
these values from the loaded physical owners.

Before a newly assembled or split transmission line enters the solver graph,
both of its boundary nodes are rebound to the current endpoint index and
verified to belong to the target electrical network. This prevents a
placeholder left over from chunk loading from being inserted as an
out-of-network wire endpoint.

## Safety bounds

- At most 8 chunks are temporarily active at once.
- A physical part's owner and endpoint chunks are never split across groups.
- At most 512 unique chunks may be included in one rebuild. A larger rebuild is
  refused rather than partially scanning the dimension.
- Each chunk group receives at most 3 load attempts.
- Each attempt has a 100-tick timeout and retries are separated by 20 ticks.
- A loaded physical owner which does not recreate its saved registration
  receives at most 3 registration attempts in total, separated by 20 ticks.
  These attempts reuse the current bounded chunk group; they do not acquire
  additional tickets.
- A matching saved registration whose physical owner and terminal structures
  are present is retained immediately. It does not consume more attempts merely
  because a late runtime electrical behaviour is not ready.
- An unresolved present owner receives one post-isolation observation window of
  at most 100 ticks. The window does not call `makeWire`, does not retry the
  rebuild and does not load chunks. It can finish early when every late
  registration has returned.
- A complete verified rebuild has a 5-minute deadline.
- Temporary tickets are explicitly released after every group, on failure, on
  dimension unload and on server shutdown. They also expire automatically.

The verified scan does not permanently force block or random ticks outside the
normal server simulation rules.

If a chunk batch times out or the verified scan otherwise aborts, the server
first attempts to assemble a consistent topology from the registrations already
available. If even that recovery step fails, all derived transmission lines are
left disconnected instead of exposing a partial graph to the solver.

## Topology retry and reporting

The command reports physical parts, unique chunks, refreshed, recovered and
removed registrations, final logical lines and elapsed time. A part which
cannot be assembled into the final topology immediately receives at most three
automatic retries, spaced 20 ticks apart. The queue processes no more than 16
retries per server tick. A part is removed from the retry queue after its third
failed attempt; it is not removed from persisted world data.

Each failed attempt logs the part identifier, endpoints, resolved nodes, last
known chunk, owner load state and failure reason. If the retry limit is
exhausted, preserve the world backup and inspect the server log before running
the command again.

Physical-owner registration retries have a separate three-attempt budget from
final topology retries. Only the unresolved identifiers are refreshed again.
An owner is not destructively refreshed while either current endpoint is absent
or structurally invalid. Runtime electrical-behaviour readiness alone does not
make a matching physical registration stale. If the budget is exhausted, the
server logs each identifier
together with the owner type and position, both current endpoints and their
validity, and the saved physical endpoint pair.

After final topology assembly, the bounded settlement window reconciles only
registrations that return naturally. A returned identifier moves into the
verified count and the final line/part totals are sampled again. An identifier
which is still absent at the deadline remains in `unresolved parts`; it is not
silently treated as a successful startup rebuild.

An error saying `Both nodes of a wire must be part of the network` during
`TransmissionLine.splitAt` indicates that a logical line retained a stale node
while its endpoint was loading. It is an electrical-topology invariant report,
not evidence that Minecraft skipped or deleted the transformer, commutator or
wire entity named later in the stack trace.

An exception saying `Prepared transmission line endpoints do not belong to the
target network` from an `ElectricBlockEntity` during a numbered verified-rebuild
batch means topology resolution escaped the isolated scan phase. It must not be
treated as a normal retry or ignored: the affected block tick can be caught by
an error-handling mod before the final rebuild completes.

`Replacing stale transmission part` is expected only when a physical owner
reports a genuinely different connection pair from its saved record. Repeated
messages for the same identifier where only a multiblock proxy coordinate
differs from its main block are not normal. They indicate that physical and
canonical endpoints were conflated. Such messages must not continue after a
successful verified rebuild when players load the affected chunks.

`Prevented a double split call` is a defensive guard report and not a thrown
entity-tick exception. It is nevertheless not expected during the final rebuild
or when a player loads network chunks. Repeated reports mean endpoint binding
has incorrectly requested another split while a line split is already
preparing its new boundaries.

Transmission lines are constructed from the non-null nodes resolved during
topology assembly. An endpoint that unloads between the verified scan and final
assembly uses its persisted placeholder node; it is never silently connected
to the graph's null/ground node.

## Recovery limit

The scan can discover unloaded physical owners from saved part records. It can
also recover a missing conductor registration when another saved conductor of
the same physical cord leads the scan to that entity.

It cannot safely discover a physical wire when every saved registration and
chunk hint for that entity was already deleted by an older repair build.
Searching every generated chunk would impose unbounded startup cost and could
generate or load unrelated terrain. Restore a backup made before those records
were lost when guaranteed recovery of that case is required.
