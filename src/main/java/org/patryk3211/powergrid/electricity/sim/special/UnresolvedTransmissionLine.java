/*
 * Copyright 2025 patryk3211
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
package org.patryk3211.powergrid.electricity.sim.special;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnresolvedTransmissionLine {
    private final IWireEndpoint endpoint1;
    private final IWireEndpoint endpoint2;
    private final double resistance;
    private final List<Segment> segments;

    private byte resolvedEndpoints = 0;
    private boolean resolved = false;

    public UnresolvedTransmissionLine(TransmissionLine line) {
        endpoint1 = line.getEndpoint1();
        endpoint2 = line.getEndpoint2();
        resistance = line.getResistance();

        segments = new ArrayList<>();
        var prevEndpoint = line.getEndpoint1();
        for(var segment : line.segments) {
            IWireEndpoint endpoint;
            endpoint = segment.endpoint2;
            if(endpoint.equals(prevEndpoint)) {
                endpoint = segment.endpoint1;
            }
            prevEndpoint = endpoint;
            var unSegment = new Segment(endpoint, segment.persistentOwnerId, segment.lastKnownChunk, segment.getResistance());
            unSegment.resolvedWire = segment;
            segments.add(unSegment);
        }
    }

    public UnresolvedTransmissionLine(CompoundTag nbt) {
        endpoint1 = WireEndpointType.deserialize(nbt.getCompound("Node1"));
        endpoint2 = WireEndpointType.deserialize(nbt.getCompound("Node2"));
        resistance = nbt.getDouble("Resistance");

        segments = new ArrayList<>();
        for(var segmentGeneric : nbt.getList("Segments", Tag.TAG_COMPOUND)) {
            var segment = (CompoundTag) segmentGeneric;
            var endpoint = WireEndpointType.deserialize(segment.getCompound("Node"));
            var id = segment.getUUID("Id");
            var resistance2 = segment.getDouble("Resistance");
            var chunkPos = new ChunkPos(segment.getInt("X"), segment.getInt("Z"));
            segments.add(new UnresolvedTransmissionLine.Segment(endpoint, id, chunkPos, resistance2));
        }
    }

    public CompoundTag writeNbt() {
        var lineEntry = new CompoundTag();
        lineEntry.put("Node1", endpoint1.serialize());
        lineEntry.put("Node2", endpoint2.serialize());
        lineEntry.putDouble("Resistance", resistance);

        var segmentList = new ListTag();
        lineEntry.put("Segments", segmentList);
        for(var segment : segments) {
            var segmentEntry = new CompoundTag();
            segmentEntry.put("Node", segment.endpoint.serialize());
            segmentEntry.putUUID("Id", segment.id);
            segmentEntry.putInt("X", segment.chunkPos.x);
            segmentEntry.putInt("Z", segment.chunkPos.z);
            segmentEntry.putDouble("Resistance", segment.resistance);
            segmentList.add(segmentEntry);
        }
        return lineEntry;
    }

    public IWireEndpoint endpoint1() {
        return endpoint1;
    }

    public IWireEndpoint endpoint2() {
        return endpoint2;
    }

    public void setEndpoint1Resolved() {
        resolvedEndpoints |= 1;
    }

    public void setEndpoint2Resolved() {
        resolvedEndpoints |= 2;
    }

    private static void shouldSplit(WorldNetworks global, ElectricalNetwork network, TransmissionLine line, OwnedFloatingNode node) {
        var line1 = global.transmissionLineNodes.get(node);
        if(line1 != null)
            line1.splitAt(node);
        if(global.connectionCount(node.endpoint) != 0) {
            // Cannot simply assign node to a line, this line must be split.
            PowerGrid.LOGGER.warn("Line has a junction in the middle.");
        } else {
            global.assignTransmissionLine(node, line);
            network.removeNode(node);
        }
    }

    public void resolve(WorldNetworks global) {
        if(resolved)
            return;
        if((resolvedEndpoints & 3) != 3)
            return;
        var network = global.prepareForConnection(endpoint1, endpoint2);
        if(network == null) {
            PowerGrid.LOGGER.error("Failed to prepare endpoints of line while resolving");
            return;
        }
        resolved = true;

        var node1 = endpoint1.getNode(global.world);
        var node2 = endpoint2.getNode(global.world);

        var line = new TransmissionLine(resistance, endpoint1, endpoint2, global);
        IWireEndpoint endpoint1, endpoint2 = this.endpoint1;
        for(var segment : segments) {
            endpoint1 = endpoint2;
            endpoint2 = segment.endpoint;
            if(segment.resolvedWire != null) {
                line.segments.add(segment.resolvedWire);
                segment.resolvedWire.setLine(line);
                if(!endpoint1.equals(this.endpoint1)) {
                    // Not the first segment
                    var node = segment.resolvedWire.getNode1();
                    shouldSplit(global, network, line, node);
                }
                if(!endpoint2.equals(this.endpoint2)) {
                    // Not the last segment
                    var node = segment.resolvedWire.getNode2();
                    shouldSplit(global, network, line, node);
                }
                continue;
            }
            var part = new TransmissionLinePart(segment.resistance, endpoint1, endpoint2, segment.id, segment.chunkPos, line);
            line.segments.add(part);
            line.unloadedParts.add(part);
            global.bounty(segment.id, segment.chunkPos, line);
        }
        global.assignTransmissionLine(node1, null);
        global.assignTransmissionLine(node2, null);
        global.removeUnresolvedLine(this);
        network = global.prepareForConnection(line.getNode1(), line.getNode2());
        if(network == null) {
            // Very bad
            PowerGrid.LOGGER.error("Failed to prepare nodes of just resolved line {}", line);
            return;
        }
        network.addWire(line);
    }

    @Nullable
    public TransmissionLinePart resolvePart(WorldNetworks global, WireEntity entity) {
        var endpoint1 = this.endpoint1;
        var endpoint2 = this.endpoint1;
        for(var segment : segments) {
            endpoint1 = endpoint2;
            endpoint2 = segment.endpoint;
            if(segment.id.equals(entity.getUUID())) {
                // Test for flipped endpoints.
                if(endpoint1.equals(entity.getEndpoint2()) && endpoint2.equals(entity.getEndpoint1())) {
                    // Flipped.
                    entity.flipEndpoints();
                }
                // This is the segment.
                var part = new TransmissionLinePart(segment.resistance, endpoint1, endpoint2, global.world, entity, null);
                segment.resolvedWire = part;
                if(endpoint1.equals(this.endpoint1)) {
                    // First segment.
                    setEndpoint1Resolved();
                }
                if(endpoint2.equals(this.endpoint2)) {
                    // Last segment.
                    setEndpoint2Resolved();
                }
                resolve(global);
                return part;
            }
        }
        return null;
    }

    public void resolveEnd(WorldNetworks global, @NotNull IWireEndpoint endpoint) {
        if(endpoint.equals(endpoint1)) {
            setEndpoint1Resolved();
            if(endpoint2.isValid(global.world)) {
                setEndpoint2Resolved();
            }
        } else if(endpoint.equals(endpoint2)) {
            setEndpoint2Resolved();
            if(endpoint1.isValid(global.world)) {
                setEndpoint1Resolved();
            }
        }

        resolve(global);
    }

    public static class Segment {
        public final IWireEndpoint endpoint;
        public final UUID id;
        public final ChunkPos chunkPos;

        public final double resistance;

        public TransmissionLinePart resolvedWire;

        public Segment(IWireEndpoint endpoint, UUID id, ChunkPos chunkPos, double resistance) {
            this.endpoint = endpoint;
            this.id = id;
            this.chunkPos = chunkPos;
            this.resistance = resistance;
        }
    }
}
