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

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.List;
import java.util.UUID;

public class UnresolvedTransmissionLine {
    private final IWireEndpoint endpoint1;
    private final IWireEndpoint endpoint2;
    private final double resistance;
    private final List<Segment> segments;

    private byte resolvedEndpoints = 0;

    public UnresolvedTransmissionLine(IWireEndpoint endpoint1, IWireEndpoint endpoint2, double resistance, List<Segment> segments) {
        this.endpoint1 = endpoint1;
        this.endpoint2 = endpoint2;
        this.resistance = resistance;
        this.segments = segments;
    }

    public void setEndpoint1Resolved() {
        resolvedEndpoints |= 1;
    }

    public void setEndpoint2Resolved() {
        resolvedEndpoints |= 2;
    }

    public void resolve(WorldNetworks global) {
        if((resolvedEndpoints & 3) != 3)
            return;
        var network = global.prepareForConnection(endpoint1, endpoint2);
        if(network == null)
            return;

        var node1 = endpoint1.getNode(global.world);
        var node2 = endpoint2.getNode(global.world);

        var line = new TransmissionLine(resistance, node1, node2, global);
        var endpoint1 = this.endpoint1;
        var endpoint2 = this.endpoint1;
        for(var segment : segments) {
            endpoint1 = endpoint2;
            endpoint2 = segment.endpoint;
            if(segment.resolvedWire != null) {
                line.segments.add(segment.resolvedWire);
                segment.resolvedWire.setLine(line);
                if(!endpoint1.equals(this.endpoint1)) {
                    // Not the first segment
                    var node = segment.resolvedWire.getNode1();
                    global.assignTransmissionLine(node, line);
                    network.removeNode(node);
                }
                if(!endpoint2.equals(this.endpoint2)) {
                    // Not the last segment
                    var node = segment.resolvedWire.getNode2();
                    global.assignTransmissionLine(node, line);
                    network.removeNode(node);
                }
                continue;
            }
            var part = new TransmissionLinePart(segment.resistance, endpoint1, endpoint2, segment.id, line);
            line.segments.add(part);
            line.unloadedParts.add(part);
        }
        global.assignTransmissionLine(node1, null);
        global.assignTransmissionLine(node2, null);
        global.unresolvedLines.remove(this);
        network.addWire(line);
    }

    @Nullable
    public TransmissionLinePart resolvePart(WorldNetworks global, WireEntity entity) {
        var endpoint1 = this.endpoint1;
        var endpoint2 = this.endpoint1;
        for(var segment : segments) {
            endpoint1 = endpoint2;
            endpoint2 = segment.endpoint;
            if(segment.id.equals(entity.getUuid())) {
                // This is the segment.
                var part = new TransmissionLinePart(segment.resistance, endpoint1.getNode(global.world), endpoint2.getNode(global.world), entity, null);
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

    public static class Segment {
        public final IWireEndpoint endpoint;
        public final UUID id;
        public final double resistance;

        public TransmissionLinePart resolvedWire;

        public Segment(IWireEndpoint endpoint, UUID id, double resistance) {
            this.endpoint = endpoint;
            this.id = id;
            this.resistance = resistance;
        }
    }
}
