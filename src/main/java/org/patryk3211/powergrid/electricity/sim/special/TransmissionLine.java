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

import net.createmod.ponder.api.level.PonderLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.*;

public class TransmissionLine extends ElectricWire implements ITransmissionLine {
    public static final boolean ENABLE_VALIDATION = false;

    public final List<TransmissionLinePart> segments = new ArrayList<>();

    private static int NEXT_ID = 0;
    private final int id;

    protected final WorldNetworks global;

    private IWireEndpoint endpoint1;
    private IWireEndpoint endpoint2;

    public TransmissionLine(double resistance, IWireEndpoint endpoint1, IWireEndpoint endpoint2, WorldNetworks global) {
        super(resistance, endpoint1.getNode(global.world), endpoint2.getNode(global.world));
        if(global.world.isClientSide && !(global.world instanceof PonderLevel))
            PowerGrid.LOGGER.warn("This method probably shouldn't be used on client-side");
        this.global = global;
        this.endpoint1 = endpoint1;
        this.endpoint2 = endpoint2;
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

    public void validateLine() {
        PowerGrid.LOGGER.info("Validating {}", this);
        if(!segments.get(0).endpoint1.equals(endpoint1)) {
            PowerGrid.LOGGER.error("Line doesn't start where its first segment starts");
        }
        if(!segments.get(segments.size() - 1).endpoint2.equals(endpoint2)) {
            PowerGrid.LOGGER.error("Line doesn't end where its last segment ends");
        }
        var prevEndpoint = endpoint1;
        for(var segment : segments) {
            if(!segment.endpoint1.equals(prevEndpoint)) {
                PowerGrid.LOGGER.error("Line not continuous, divergence between {} and {}", segment.endpoint1, prevEndpoint);
            }
            prevEndpoint = segment.endpoint2;
        }
    }

    @Override
    public WorldNetworks global() {
        return global;
    }

    public void setNode1(@NotNull IWireEndpoint endpoint, OwnedFloatingNode node) {
        this.endpoint1 = endpoint;
        super.setNode1(node);
    }

    public void setNode2(@NotNull IWireEndpoint endpoint, OwnedFloatingNode node) {
        this.endpoint2 = endpoint;
        super.setNode2(node);
    }

    @Override
    public void setNode1(IElectricNode node1) {
        assert node1 instanceof OwnedFloatingNode;
        super.setNode1(Objects.requireNonNull(node1));
        endpoint1 = ((OwnedFloatingNode) node1).endpoint;
    }

    @Override
    public void setNode2(IElectricNode node2) {
        assert node2 instanceof OwnedFloatingNode;
        super.setNode2(Objects.requireNonNull(node2));
        endpoint2 = ((OwnedFloatingNode) node2).endpoint;
    }

    @Override
    public void flipNodes() {
        super.flipNodes();
        var endpoint = endpoint1;
        endpoint1 = endpoint2;
        endpoint2 = endpoint;
    }

    @Override
    public OwnedFloatingNode getNode1() {
        return (OwnedFloatingNode) node1;
    }

    @Override
    public OwnedFloatingNode getNode2() {
        return (OwnedFloatingNode) node2;
    }

    @Override
    public IWireEndpoint getEndpoint1() {
        return endpoint1;
    }

    @Override
    public IWireEndpoint getEndpoint2() {
        return endpoint2;
    }

    @Override
    public void grabPart(@NotNull WireEntity owner, TransmissionLinePart part) {
        if(part.persistentOwnerId.equals(owner.getUUID())) {
            // TODO: This is failing because nodes are not updated
            // If the owner id matches then the endpoints should match too
            var endpointArrangement = validateEndpoints(part, owner);
            if(endpointArrangement == 1 || endpointArrangement == 2) {
                part.setNode1(part.endpoint1.getNode(owner.level()));
                part.setNode2(part.endpoint2.getNode(owner.level()));
                if(endpointArrangement == 2)
                    owner.flipEndpoints();
            } else {
                PowerGrid.LOGGER.warn("Endpoint of wire and unloaded line segment do not match\n{}, {} vs {}, {}",
                        part.endpoint1, part.endpoint2, owner.getEndpoint1(), owner.getEndpoint2());
                remove();
                return;
            }
            if(part.getEndpoint1().equals(endpoint1) && endpoint1.getNode(global.world) != node1) {
                // Update node
                setNode1(endpoint1.getNode(global.world));
            }
            if(part.getEndpoint2().equals(endpoint2) && endpoint2.getNode(global.world) != node2) {
                // Update node
                setNode2(endpoint2.getNode(global.world));
            }
            if(part.getResistance() != owner.getResistance()) {
                var diff = owner.getResistance() - part.getResistance();
                part.setResistance(owner.getResistance());
                setResistance(getResistance() + diff);
            }
            if(part.getNode2() != node2) {
                global.assignTransmissionLine(part.getNode2(), this);
            }
            if(part.getNode1() != node1) {
                global.assignTransmissionLine(part.getNode1(), this);
            }
            if(ENABLE_VALIDATION)
                validateLine();
            PowerGrid.LOGGER.debug("{}: Grabbed unloaded part of line, owner={}, between=({}, {})", this, owner, part.getNode1(), part.getNode2());
        }
    }

    public void addLastSegment(TransmissionLinePart wire) {
        assert wire.getNode1() == getNode2();
        wire.setLine(this);
        segments.add(wire);
        setResistance(resistance + wire.getResistance());
        network.removeNode(getNode2());

        // Move boundary
        global.assignTransmissionLine(getNode2(), this);
        setNode2(wire.endpoint2, wire.getNode2());
        global.assignTransmissionLine(getNode2(), null);
        if(ENABLE_VALIDATION)
            validateLine();
    }

    public void addFirstSegment(TransmissionLinePart wire) {
        assert wire.getNode2() == getNode1();
        wire.setLine(this);
        segments.add(0, wire);
        setResistance(resistance + wire.getResistance());
        network.removeNode(getNode1());

        // Move boundary
        global.assignTransmissionLine(getNode1(), this);
        setNode1(wire.endpoint1, wire.getNode1());
        global.assignTransmissionLine(getNode1(), null);
        if(ENABLE_VALIDATION)
            validateLine();
    }

    public void merge(TransmissionLine line) {
        assert getNode2() == line.getNode1();
        PowerGrid.LOGGER.debug("{}: Appending {}", this, line);
        segments.addAll(line.segments);
        setResistance(resistance + line.getResistance());
        global.assignTransmissionLine(line.getNode1(), this);
        segments.forEach(part -> {
            global.assignTransmissionLine(part.getNode2(), this);
            if(part.owner == null || part.owner.isRemoved())
                global.bounty(part.persistentOwnerId, part.lastKnownChunk);
            part.setLine(this);
        });
        global.assignTransmissionLine(line.getNode2(), null);
        line.segments.clear();
        setNode2(line.endpoint2, line.getNode2());
        line.remove();
        if(ENABLE_VALIDATION)
            validateLine();
    }

    @Nullable
    public TransmissionLine splitAt(OwnedFloatingNode atNode) {
        if(atNode == node1 || atNode == node2 || endpoint1.equals(atNode.endpoint) || endpoint2.equals(atNode.endpoint))
            return null;
        if(segments.size() <= 1)
            return null;
        PowerGrid.LOGGER.debug("{}: Splitting transmission line between {} and {} at {}", this, node1, node2, atNode);
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
                    line2 = new TransmissionLine(1, segment.endpoint2, endpoint2, global);
                    global.assignTransmissionLine(splitNode, null);
                    global.inNetwork(network, splitNode);
                    setNode2(segment.getEndpoint2(), splitNode);
                } else if(segment.getEndpoint2().equals(atNode.endpoint)) {
                    // Nodes do not match but the endpoint does. Split should occur.
                    line2 = new TransmissionLine(1, segment.endpoint2, endpoint2, global);
                    // Get the most up-to-date node.
                    var newNode = atNode.endpoint.getNode(global.world);
                    global.assignTransmissionLine(newNode, null);
                    global.assignTransmissionLine(splitNode, null);
                    global.assignTransmissionLine(atNode, null);
                    global.inNetwork(network, newNode);
                    setNode2(segment.getEndpoint2(), newNode);
                }
            } else {
                R2 += segment.getResistance();
                segment.setLine(line2);
                line2.segments.add(segment);
                global.assignTransmissionLine(segment.getNode2(), line2);
                if(segment.owner == null || segment.owner.isRemoved())
                    global.bounty(segment.persistentOwnerId, segment.lastKnownChunk);
                iter.remove();
            }
        }
        setResistance(R1);
        if(line2 != null && R2 > 0) {
            line2.setResistance(R2);
            global.assignTransmissionLine(line2.getNode2(), null);
            var network = global.prepareForConnection(line2.endpoint1, line2.endpoint2);
            if(network != null)
                network.addWire(line2);
        } else {
            throw new IllegalStateException("Splitting failed for " + this);
        }
        if(ENABLE_VALIDATION)
            validateLine();
        return line2;
    }

    public void flip() {
        var segmentsCopy = List.copyOf(segments);
        PowerGrid.LOGGER.debug("{}: Flipping transmission line between {} and {}", this, node1, node2);
        segments.clear();
        for(var segment : segmentsCopy) {
            segment.flipNodes();
            segments.add(0, segment);
        }
        flipNodes();
        if(ENABLE_VALIDATION)
            validateLine();
    }

    @Override
    public void remove() {
        super.remove();
        PowerGrid.LOGGER.debug("{}: Removing transmission line between {} and {}", this, node1, node2);
        for(var segment : segments) {
            global.assignTransmissionLine(segment.getNode1(), null);
            global.assignTransmissionLine(segment.getNode2(), null);
            global.unregisterPart(segment.persistentOwnerId, segment);
            segment.setLine(null);
        }
        optimizeNode(getNode1());
        optimizeNode(getNode2());
    }

    public void unresolve() {
        super.remove();
        PowerGrid.LOGGER.debug("{}: Unresolving transmission line between {} and {}", this, node1, node2);
        for(var segment : segments) {
            global.assignTransmissionLine(segment.getNode1(), null);
            global.assignTransmissionLine(segment.getNode2(), null);
            segment.setLine(null);
        }
        if(ENABLE_VALIDATION)
            validateLine();
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

    @Override
    public void remove(TransmissionLinePart wire) {
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
            PowerGrid.LOGGER.debug("{}: Removing first segment of transmission line", this);
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
            global.inNetwork(network, node1);
            var optiNode = getNode1();
            setNode1(wire.endpoint2, node1);
            optimizeNode(optiNode);
            if(ENABLE_VALIDATION)
                validateLine();
        } else if (wire.getNode2() == node2) {
            // Last segment
            PowerGrid.LOGGER.debug("{}: Removing last segment of transmission line", this);
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
            global.inNetwork(network, node2);
            var optiNode = getNode2();
            setNode2(wire.endpoint1, node2);
            optimizeNode(optiNode);
            if(ENABLE_VALIDATION)
                validateLine();
        } else {
            // Middle segment
            PowerGrid.LOGGER.debug("{}: Removing middle segment of transmission line", this);
            var splitNode = wire.getNode2();
            var line2 = splitAt(splitNode);
            if(line2 == null)
                return;
            if(ENABLE_VALIDATION)
                line2.validateLine();

            global.assignTransmissionLine(splitNode, null);
            global.inNetwork(network, splitNode);

            // Last segment is the removed wire.
            var removed = segments.remove(segments.size() - 1);
            assert removed == wire;
            removed.setLine(null);
            var terminatingNode = wire.getNode1();
            global.assignTransmissionLine(terminatingNode, null);
            if(segments.isEmpty()) {
                PowerGrid.LOGGER.debug("{}: Split and remove resulted in an empty line", this);
                remove();
                return;
            }
            setResistance(resistance - removed.getResistance());
            global.inNetwork(network, terminatingNode);
            setNode2(wire.endpoint1, terminatingNode);
            if(ENABLE_VALIDATION)
                validateLine();
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
