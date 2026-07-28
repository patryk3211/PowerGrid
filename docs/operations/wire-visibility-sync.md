---
title: Wire visibility synchronization
---

# Wire visibility synchronization

Power Grid wires are persistent entities. Their electrical state is authoritative
on the server, while their item, endpoints and render geometry are sent to each
client through the existing Power Grid entity-data packet.

Minecraft sends the wire entity's spawn bundle first. The server then sends the
complete wire data directly to that exact player, followed by a second packet
containing the terminal positions already calculated by the server. Render-only
geometry broadcasts are restricted to players whose tracking session has
completed. All packets use formats already supported by the official client.

The ordering is intentional. The official client can retain one entity-data
packet while waiting for the entity with that numeric id to spawn. A render-only
geometry packet does not contain the wire item, so applying it to a fresh entity
can fail during render setup. Excluding not-yet-paired players from ordinary
geometry broadcasts prevents that packet from arriving before spawn. Once
spawned, complete material and endpoint state is sent before render geometry.
Players are removed from the geometry recipient set before Minecraft ends their
tracking session, preventing a late geometry packet from surviving the removal.

This applies whenever the player starts tracking:

- hanging wires;
- block-mounted wires;
- power cords;
- string-light cords.

Dedicated-server clients do not need this change for protocol compatibility.
Packet formats, packet registration and the entity registry are unchanged.
The same universal mod jar still contains the server logic when playing in an
integrated single-player server.

The synchronization is event-driven. It sends one full-data packet after the
spawn bundle during pairing with a wire. Hanging wires, power cords and
string-light cords also receive one existing terminal-geometry packet
immediately afterward. Block-mounted wires need only the full packet because
their segment geometry is stored directly in that data. The repair does not add
a periodic retry or broadcast loop. Normal geometry updates are sent only to
the entity's established tracking sessions.

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

After the repair, full material and endpoint data arrives before terminal
geometry for every completed pairing. The server-calculated geometry therefore
does not depend on client chunk or block-entity timing. Updated clients also
retain complete deferred data when a render-only packet arrives out of order,
ignore geometry until wire material exists, and clear deferred entity data when
leaving a world. These client guards complement the server-side ordering but
are not required for an official client connecting to a repaired server.

This repair does not:

- rebuild electrical topology;
- change current flow or resistance;
- load or retain chunks;
- increase entity tracking distance;
- change packet formats or require a modified client.

If a wire remains invisible for every player while its complete entity bounding
box is visible with Minecraft's entity-hitbox debug view, investigate a
separate client-side renderer or culling issue instead.
