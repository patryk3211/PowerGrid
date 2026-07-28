---
title: Wire visibility synchronization
---

# Wire visibility synchronization

Power Grid wires are persistent entities. Their electrical state is authoritative
on the server, while their item, endpoints and render geometry are sent to each
client through the existing Power Grid entity-data packet.

The server sends the complete wire data directly to a player immediately before
Minecraft sends that wire entity's spawn bundle. Hanging wires and cords then
receive a second packet containing the exact terminal positions already
calculated by the server, after the spawn bundle has been queued. Both packets
use formats already supported by the official client.

The ordering is intentional. The official client can retain one entity-data
packet while waiting for the entity with that numeric id to spawn. A render-only
geometry packet left behind by an earlier tracking session does not contain the
wire item, so applying it to a fresh entity can fail during render setup. The
new complete pre-spawn packet replaces any such stale entry, initializes the
wire item and endpoints when the entity joins, and only then is the render-only
geometry sent.

This applies whenever the player starts tracking:

- hanging wires;
- block-mounted wires;
- power cords;
- string-light cords.

Dedicated-server clients do not need this change for protocol compatibility.
Packet formats, packet registration and the entity registry are unchanged.
The same universal mod jar still contains the server logic when playing in an
integrated single-player server.

The synchronization is event-driven. It sends one full-data packet during
pairing with a wire. Hanging wires, power cords and string-light cords also send
one existing terminal-geometry packet after pairing. Block-mounted wires need
only the full packet because their segment geometry is stored directly in that
data. The repair does not add a periodic retry or broadcast loop. Normal wire
updates continue to use the existing tracking broadcast.

## Observable behavior

A client which previously received the entity spawn without usable render data
could see an invisible wire even though the server still simulated current
through it. Relogging or another player observing the same location could make
the wire reappear because each client has independent entity-tracking state.

The earlier repair sent only the persisted endpoint descriptions. During chunk
arrival, a client can know those block coordinates before the corresponding
electrical blocks or block entities are ready. It then substitutes block
centres for the real terminal offsets. For a taut wire, that temporary geometry
can be longer than the saved placed length, producing an invalid curve; the
official client removes that local wire entity on its next tick. The server
entity and electrical connection remain healthy, which explains why power
continues and why visibility can differ between players.

After the repair, full material and endpoint data still arrives first. The
server-calculated terminal geometry follows the spawn bundle, so curve
initialization does not depend on client chunk or block-entity timing and a
geometry-only packet cannot replace the full deferred initialization packet.

This repair does not:

- rebuild electrical topology;
- change current flow or resistance;
- load or retain chunks;
- increase entity tracking distance;
- change any client class or resource.

If a wire remains invisible for every player while its complete entity bounding
box is visible with Minecraft's entity-hitbox debug view, investigate a
separate client-side renderer or culling issue instead.
