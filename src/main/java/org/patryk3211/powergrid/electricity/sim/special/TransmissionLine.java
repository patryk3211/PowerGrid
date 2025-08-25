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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.*;

public class TransmissionLine extends ElectricWire {
    public final List<TransmissionLinePart> segments = new ArrayList<>();
//    protected final Set<TransmissionLinePart> unloadedParts = new HashSet<>();

    private static int NEXT_ID = 0;
    private final int id;

    private final WorldNetworks global;

    protected TransmissionLine(double resistance, OwnedFloatingNode node1, OwnedFloatingNode node2, WorldNetworks global) {
        super(resistance, node1, node2);
        if(global.world.isClientSide)
            PowerGrid.LOGGER.warn("This method probably shouldn't be used on client-side");
        this.global = global;
        id = NEXT_ID++;
    }

    public TransmissionLine(double resistance, OwnedFloatingNode node1, OwnedFloatingNode node2, TransmissionLinePart firstSegment, WorldNetworks global) {
        super(resistance, node1, node2);
        if(global.world.isClientSide)
            PowerGrid.LOGGER.warn("This method probably shouldn't be used on client-side");
        segments.add(firstSegment);
        this.global = global;
        id = NEXT_ID++;
    }

    // This should only be utilized by the client
    public TransmissionLine(int id, double resistance, OwnedFloatingNode node1, OwnedFloatingNode node2, WorldNetworks global) {
        super(resistance, node1, node2);
        if(!global.world.isClientSide)
            PowerGrid.LOGGER.warn("This method probably shouldn't be used on server-side");
        this.global = global;
        this.id = id;
    }

    private int validateEndpoints(TransmissionLinePart part, WireEntity owner) {
        if(part.endpoint1.equals(owner.getEndpoint1())) {
            if(part.endpoint2.equals(owner.getEndpoint2())) {
                return 1;
            }
        } else if(part.endpoint2.equals(owner.getEndpoint1())) {
            // Endpoints are flipped
            if(part.endpoint1.equals(owner.getEndpoint2())) {
                return 2;
            }
        }
        return 0;
    }

    @Override
    public void setNode1(IElectricNode node1) {
        assert node1 instanceof OwnedFloatingNode;
        super.setNode1(Objects.requireNonNull(node1));
    }

    @Override
    public void setNode2(IElectricNode node2) {
        assert node2 instanceof OwnedFloatingNode;
        super.setNode2(Objects.requireNonNull(node2));
    }

    @Override
    public OwnedFloatingNode getNode1() {
        return (OwnedFloatingNode) node1;
    }

    @Override
    public OwnedFloatingNode getNode2() {
        return (OwnedFloatingNode) node2;
    }

    @Nullable
    public TransmissionLinePart grabUnloaded(@NotNull WireEntity owner) {
        for(var part : segments) {
            if(part.persistentOwnerId.equals(owner.getUUID())) {
                // If the owner id matches then the endpoints should match too
                var endpointArrangement = validateEndpoints(part, owner);
                if(endpointArrangement == 1 || endpointArrangement == 2) {
                    part.setNode1(part.endpoint1.getNode(owner.level()));
                    part.setNode2(part.endpoint2.getNode(owner.level()));
                    if(endpointArrangement == 2)
                        owner.flipEndpoints();
                } else {
                    PowerGrid.LOGGER.error("Endpoint of wire and unloaded line segment do not match");
                    return null;
                }
                part.owner = owner;
                if(part.getResistance() != owner.getResistance()) {
                    var diff = owner.getResistance() - part.getResistance();
                    part.setResistance(owner.getResistance());
                    setResistance(getResistance() + diff);
                }
//                unloadedParts.remove(part);
                if(part.getNode2() != node2) {
                    global.assignTransmissionLine(part.getNode2(), this);
                }
                if(part.getNode1() != node1) {
                    global.assignTransmissionLine(part.getNode1(), this);
                }
                return part;
            }
        }
        return null;
    }

    public void unloadPart(TransmissionLinePart part) {
        // Save endpoints and drop owner
        assert part.getNode1() != null && part.getNode2() != null;
//        part.endpoint1 = part.getNode1().endpoint;
//        part.endpoint2 = part.getNode2().endpoint;
        part.owner = null;
        global.bounty(part.persistentOwnerId, this);
//        unloadedParts.add(part);
    }

    public void addLastSegment(TransmissionLinePart wire) {
        assert wire.getNode1() == getNode2();
        wire.setLine(this);
        segments.add(wire);
        setResistance(resistance + wire.getResistance());
        network.removeNode(getNode2());

        // Move boundary
        global.assignTransmissionLine(getNode2(), this);
        setNode2(wire.getNode2());
        global.assignTransmissionLine(getNode2(), null);
    }

    public void addFirstSegment(TransmissionLinePart wire) {
        assert wire.getNode2() == getNode1();
        wire.setLine(this);
        segments.add(0, wire);
        setResistance(resistance + wire.getResistance());
        network.removeNode(getNode1());

        // Move boundary
        global.assignTransmissionLine(getNode1(), this);
        setNode1(wire.getNode1());
        global.assignTransmissionLine(getNode1(), null);
    }

    public void merge(TransmissionLine line) {
        assert getNode2() == line.getNode1();
        PowerGrid.LOGGER.trace("{}: Appending {}", this, line);
        segments.addAll(line.segments);
        setResistance(resistance + line.getResistance());
        global.assignTransmissionLine(line.getNode1(), this);
        segments.forEach(part -> {
            global.assignTransmissionLine(part.getNode2(), this);
            part.setLine(this);
        });
        global.assignTransmissionLine(line.getNode2(), null);
        line.segments.clear();
        setNode2(line.getNode2());
        line.remove();
    }

    @Nullable
    public TransmissionLine splitAt(IElectricNode atNode) {
        if(atNode == node1 || atNode == node2)
            return null;
        if(segments.size() == 1)
            return null;
        PowerGrid.LOGGER.trace("{}: Splitting transmission line between {} and {} at {}", this, node1, node2, atNode);
        TransmissionLine line2 = null;
        double R1 = 0, R2 = 0;
        var iter = segments.iterator();
        while (iter.hasNext()) {
            var segment = iter.next();
            if (line2 == null) {
                R1 += segment.getResistance();
                var splitNode = segment.getNode2();
                if (splitNode == atNode) {
                    // This is the last segment of this line.
                    // All other segments go to the next line.
                    line2 = new TransmissionLine(1, splitNode, getNode2(), global);
                    global.assignTransmissionLine(splitNode, null);
                    if(network != null)
                        network.addNode(splitNode);
                    setNode2(splitNode);
                }
            } else {
                R2 += segment.getResistance();
                line2.segments.add(segment);
                segment.setLine(line2);
                global.assignTransmissionLine(segment.getNode2(), line2);
                iter.remove();
            }
        }
        setResistance(R1);
        if(line2 != null) {
            line2.setResistance(R2);
            global.assignTransmissionLine(line2.getNode2(), null);
            if(network != null)
                network.addWire(line2);
        } else {
            PowerGrid.LOGGER.trace("{}:   Splitting failed", this);
            global.assignTransmissionLine(getNode2(), null);
        }
        return line2;
    }

    public void flip() {
        var segmentsCopy = List.copyOf(segments);
        PowerGrid.LOGGER.trace("{}: Flipping transmission line between {} and {}", this, node1, node2);
        segments.clear();
        for(var segment : segmentsCopy) {
            segment.flipNodes();
            segments.add(0, segment);
        }
        flipNodes();
    }

    @Override
    public void remove() {
        super.remove();
        PowerGrid.LOGGER.trace("{}: Removing transmission line between {} and {}", this, node1, node2);
        for(var segment : segments) {
            global.assignTransmissionLine(segment.getNode2(), null);
        }
        optimizeNode(getNode1());
        optimizeNode(getNode2());
    }

    public boolean isPart(ElectricWire wire) {
        return segments.contains(wire);
    }

    private void optimizeNode(IElectricNode node) {
        if(global.world.isClientSide)
            return;
        if(global.globalGraph.connectionCount(node) == 2) {
            // We can possibly merge two transmission lines here.
            var nodes = global.globalGraph.getConnectedNodes(node);
            if(nodes.size() != 2)
                return;
            var lines = nodes.stream()
                    .map(connected -> global.globalGraph.getFirstWire(node, connected))
                    .filter(wire -> wire instanceof TransmissionLine)
                    .map(wire -> (TransmissionLine) wire)
                    .toList();
            if(lines.size() != 2)
                return;
            var line1 = lines.get(0);
            var line2 = lines.get(1);
            if(line1.getNetwork() != line2.getNetwork())
                return;
            if(line1.getNode1() == line2.getNode2()) {
                // Append line1 onto line2
                line2.merge(line1);
            } else if(line1.getNode2() == line2.getNode1()) {
                // Append line2 onto line1
                line1.merge(line2);
            } else if(line1.getNode1() == line2.getNode1()) {
                line1.flip();
                line1.merge(line2);
            } else if(line1.getNode2() == line2.getNode2()) {
                line2.flip();
                line1.merge(line2);
            } else {
                PowerGrid.LOGGER.error("Unknown line optimization case");
            }
        }
    }

    public void removeSegment(TransmissionLinePart wire) {
        if(segments.isEmpty()) {
            PowerGrid.LOGGER.error("Cannot remove segments from an empty transmission line. How is it even here?");
            remove();
            return;
        }
        if(!segments.contains(wire)) {
            PowerGrid.LOGGER.error("Wire is not part of this transmission line even though it thinks so");
            return;
        }
        if (wire.getNode1() == node1) {
            // First segment
            PowerGrid.LOGGER.trace("{}: Removing first segment of transmission line", this);
            var removed = segments.remove(0);
            assert removed == wire;
            removed.setLine(null);
            if(segments.isEmpty()) {
                remove();
                return;
            }
            setResistance(resistance - removed.getResistance());

            var node1 = wire.getNode2();
            global.assignTransmissionLine(node1, null);
            if(network != null)
                network.addNode(node1);
            var optiNode = getNode1();
            setNode1(node1);
            optimizeNode(optiNode);
        } else if (wire.getNode2() == node2) {
            // Last segment
            PowerGrid.LOGGER.trace("{}: Removing last segment of transmission line", this);
            var removed = segments.remove(segments.size() - 1);
            assert removed == wire;
            removed.setLine(null);
            if(segments.isEmpty()) {
                remove();
                return;
            }
            setResistance(resistance - removed.getResistance());

            var node2 = wire.getNode1();
            global.assignTransmissionLine(node2, null);
            if(network != null)
                network.addNode(node2);
            var optiNode = getNode2();
            setNode2(node2);
            optimizeNode(optiNode);
        } else {
            // Middle segment
            PowerGrid.LOGGER.trace("{}: Removing middle segment of transmission line", this);
            var splitNode = wire.getNode2();
            var line2 = splitAt(splitNode);
            if(line2 == null)
                return;

            global.assignTransmissionLine(splitNode, null);
            if(network != null)
                network.addNode(splitNode);

            // Last segment is the removed wire.
            var removed = segments.remove(segments.size() - 1);
            assert removed == wire;
            removed.setLine(null);
            var terminatingNode = wire.getNode1();
            global.assignTransmissionLine(terminatingNode, null);
            if(segments.isEmpty()) {
                PowerGrid.LOGGER.trace("{}: Split and remove resulted in an empty line", this);
                remove();
                return;
            }
            setResistance(resistance - removed.getResistance());
            if(network != null)
                network.addNode(terminatingNode);
            setNode2(terminatingNode);
        }
    }

    @Override
    public String toString() {
        return String.format("TransmissionLine[id=%d]", id);
    }

    public int getId() {
        return id;
    }
}
