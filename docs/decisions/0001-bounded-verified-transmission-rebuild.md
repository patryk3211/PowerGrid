---
title: "ADR 0001: Bounded verified transmission rebuild"
status: accepted
date: 2026-07-28
---

# ADR 0001: Bounded verified transmission rebuild

## Context

Power Grid persists physical transmission-part registrations separately from
the optimized logical lines used by the electrical solver. Long-distance
networks can start with only part of their physical entities loaded. Older
recovery behavior could compare stale saved endpoints with a newly loaded
entity and remove the complete optimized line, orphaning otherwise healthy
segments. Rebuilding only the optimized topology could not correct a stale
physical-part registration.

Loading every generated chunk is not acceptable. It has unbounded CPU, memory
and disk I/O cost, may activate unrelated machinery, and cannot distinguish
network chunks from unrelated terrain when no registration hint remains.

## Decision

Each server dimension runs a verified rebuild once during startup. Players may
also request the same operation with `/powergrid rebuild`.

The operation is a bounded server-thread state machine:

- Persisted part records provide the discovery set.
- Each atomic work unit contains a physical part's owner and endpoint chunks.
- Units are greedily grouped under an eight-active-chunk limit.
- Derived transmission lines are detached before the first group is loaded.
  Island discovery, transmission ticks, solver work and state synchronization
  remain paused for the dimension until a complete topology is available.
- Temporary entity-ticking chunk tickets are acquired for one group at a time.
- Loaded physical entities replace their prior logical registrations using
  their current endpoints as authoritative data, but those registrations stay
  detached from the solver graph during the scan. A present owner is refreshed
  only when both of its current endpoints exist and validate against their
  loaded electrical behaviours.
- When the physical owner and saved endpoint pair match, terminal structure is
  validated separately from late runtime electrical-behaviour readiness. The
  saved registration is retained and reattached to the current owner if those
  physical terminals still exist. Placeholder nodes keep it available to final
  topology assembly until normal block initialization supplies the live nodes.
- Electrical block initialization may refresh endpoint aliases and node
  indexes during the scan, but tree tracing, line creation and line splitting
  remain disabled until final topology assembly.
- A stale endpoint mismatch replaces only the affected physical part.
- A registration with no physical owner is removed only after its complete
  chunk group was verified.
- All temporary tickets are released before derived topology is rebuilt.
- The endpoint index, node-to-part index and optimized logical lines are then
  recreated once from the refreshed registrations.
- Rebuilt transmission lines receive the already resolved, non-null endpoint
  nodes. Persisted placeholder nodes remain valid when endpoint chunks unload
  before final assembly.
- A real node from a newly loaded endpoint supersedes its persisted
  placeholder. New and split transmission lines are rebound to the endpoint
  index after connection preparation and are admitted to the solver graph only
  when both exact boundary nodes belong to the selected electrical network.
- When a real terminal node unloads, it is replaced atomically with a passive
  placeholder without detaching its healthy derived transmission line. The
  existing line remains in the solver network and global graph, while every
  endpoint alias and node-to-part index entry is migrated by identity.
  Reloading the endpoint migrates the same line back to the real node without a
  global rebuild.
- Multiblock proxy endpoints may index a node whose canonical endpoint belongs
  to the main block. This alias is preserved and the exact selected node is
  repaired or merged into the target network before line admission.
- A transmission segment stores its physical connection pair independently
  from its canonical electrical nodes. Physical endpoints remain authoritative
  for owner validation, chunk planning and persistence; topology traversal uses
  canonical node identity. Alias index entries are retained while their proxy
  blocks are loaded instead of being removed during node migration.
- Ordinary endpoint registration binds and migrates nodes without implicitly
  splitting a transmission line. Proxy-alias initialization requests a split
  explicitly and only while topology resolution is enabled. Connection
  preparation can therefore bind line boundaries without recursively
  re-entering `TransmissionLine.splitAt`.
- Solver and island-discovery work resumes only after final assembly succeeds.
  An aborted scan restores a complete topology from the available records or,
  if restoration itself fails, leaves transmission disconnected.
- A present owner which is still waiting for late electrical-behaviour
  initialization after its third registration attempt is observed for at most
  100 ticks after isolation ends. This settlement phase does not invoke a
  fourth refresh, retain tickets or extend the topology retry budget. It only
  reconciles registrations recreated by the normal entity lifecycle.
- The startup operation is server-driven and has no player-presence,
  player-position or login precondition. Its temporary entity-ticking tickets,
  structural validation and placeholder nodes are sufficient to complete a
  matching persisted network while no player is connected.
- Normal placement remains incremental. A newly registered physical part stays
  owned by its wire entity when its endpoint nodes are not ready on the first
  connection attempt, and only that part receives at most three delayed
  resolution attempts. Electrical block activation schedules island discovery
  after its internal circuit has joined the endpoint networks. Discovery
  requests produced during a running pass are carried into the next tick rather
  than discarded.

The scan is refused above 512 unique chunks. Chunk loading has three attempts
per group, a 100-tick attempt timeout, a 20-tick retry delay and a five-minute
overall deadline. A physical owner which is present but fails to recreate its
registration receives at most three total attempts, separated by 20 ticks and
using the already active group tickets. Final topology assembly retains a
separate limit of three automatic retries per unresolved persisted part.

Only one verified rebuild may run per dimension. A manual request does not
supersede an active startup rebuild.

## Consequences

The rebuild repairs both stale physical-part registrations and derived logical
topology without permanently loading the network. At most eight network chunks
are held active at a time, limiting memory and tick load. Transmission is
intentionally interrupted for the full verified scan so no partial topology can
be observed by the solver.

Endpoint loading can replace placeholder node objects without leaving an
optimized line attached to the superseded object. A violated node-membership
invariant now stops that individual topology operation before it can add an
inconsistent wire to the solver.

Normal chunk unload/reload cycles no longer orphan physical segments under a
discarded terminal-node key or dismantle the healthy logical line which joins
them. They neither keep network chunks loaded nor invoke the bounded verified
rebuild. The compact derived topology stays resident and only its boundary node
is exchanged when the endpoint leaves or returns.

A startup batch no longer gives a present physical owner only one opportunity
to recreate its registration. The separate three-attempt budget is bounded and
diagnostic: exhaustion reports the exact identifiers, current endpoint
validity and saved endpoints, while all temporary tickets are still released
normally. The short post-isolation settlement window accounts for owners whose
late behaviours become usable only after the scan releases isolation, without
turning the window into an unbounded or hidden retry mechanism.

A generator commutator or another late-initialized electrical block no longer
causes a valid matching wire record to be deleted during headless startup.
Structural terminal validation preserves the persisted segment independently
of whether any player has logged in. A missing block, missing terminal, absent
owner or changed endpoint pair still follows the bounded retry and diagnostic
path.

Loading a multiblock proxy no longer makes a healthy segment appear stale merely
because the proxy block and the canonical node owner have different positions.
The first rebuild after upgrading refreshes the existing saved records from
their physical owners; later chunk loads reuse those physical identities
without repeatedly replacing the segment.

Loading a physical wire or rebuilding a line no longer requests a nested split
while an outer split is preparing its replacement lines. The existing defensive
guard remains available for unrelated invariant violations without producing
routine stack traces during chunk loading.

New cables and electrical blocks no longer depend on a later verified rebuild
to become part of a previously rebuilt network. The bounded per-part retry
handles only transient endpoint initialization and retains normal removal and
unload ownership. Deferred island discovery makes topology changes visible to
the solver without introducing a full-network rebuild on every placement.

Startup takes longer in proportion to the number of discovered groups. The hard
limits turn excessive load or a stuck chunk into a visible failure with all
temporary tickets released.

The operation cannot recover a physical entity for which all persisted part
records and chunk hints were already lost. A pre-loss world backup remains the
authoritative recovery mechanism for that case.

## Compatibility and rollback

The topology and rebuild changes run on the logical server and do not add or
alter network packets, so unmodified clients of the matching mod version remain
protocol-compatible on a dedicated server. In single player the universal mod
jar runs the same logic in the integrated server.

The physical connection pair continues to use the existing `Node1` and `Node2`
saved-data fields. Rolling back to the official mod therefore preserves
physical entities and reads the saved part data without a schema migration.
The automatic verified rebuild and the manual repair command are no longer
available after rollback.
