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
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class TransmissionLine extends ElectricWire {
    public static final boolean ENABLE_VALIDATION = false;

    public final List<TransmissionLinePart> segments = new ArrayList<>();

    private static int NEXT_ID = 0;
    private final int id;

    protected final WorldNetworks global;

    private IWireEndpoint endpoint1;
    private IWireEndpoint endpoint2;

    private TransmissionLinePort port1;
    private TransmissionLinePort port2;

    private boolean splitting = false;

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

    private int validateEndpoints(TransmissionLinePart part, BaseWireEntity owner) {
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

    public WorldNetworks global() {
        return global;
    }

    public void setNode1(@NotNull IWireEndpoint endpoint, OwnedFloatingNode node) {
        if(getNetwork() == null && port1 != null) {
            global.globalGraph.disconnect(node1, node2, this);
            global.globalGraph.connect(node, node2, this);
            makePortPair();
        }
        this.endpoint1 = endpoint;
        super.setNode1(node);
    }

    public void setNode2(@NotNull IWireEndpoint endpoint, OwnedFloatingNode node) {
        if(getNetwork() == null && port2 != null) {
            global.globalGraph.disconnect(node1, node2, this);
            global.globalGraph.connect(node1, node, this);
            makePortPair();
        }
        this.endpoint2 = endpoint;
        super.setNode2(node);
    }

    @Override
    public void setNode1(IElectricNode node1) {
        assert node1 instanceof OwnedFloatingNode;
        var node = (OwnedFloatingNode) node1;
        setNode1(node.endpoint, Objects.requireNonNull(node));
    }

    @Override
    public void setNode2(IElectricNode node2) {
        assert node2 instanceof OwnedFloatingNode;
        var node = (OwnedFloatingNode) node1;
        setNode2(node.endpoint, Objects.requireNonNull(node));
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

    public IWireEndpoint getEndpoint1() {
        return endpoint1;
    }

    public IWireEndpoint getEndpoint2() {
        return endpoint2;
    }

    public void grabPart(@NotNull BaseWireEntity owner, TransmissionLinePart part) {
        if(part.persistentOwnerId.equals(owner.getUUID())) {
            // If the owner id matches then the endpoints should match too
            var endpointArrangement = validateEndpoints(part, owner);
            if(endpointArrangement == 1 || endpointArrangement == 2) {
                var node1 = part.endpoint1.getNode(owner.level());
                var node2 = part.endpoint2.getNode(owner.level());
                global.movePartMap(part.getNode1(), node1, part);
                global.movePartMap(part.getNode2(), node2, part);
                part.setNode1(node1);
                part.setNode2(node2);
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
            if(ENABLE_VALIDATION)
                validateLine();
            if(ModdedConfigs.logsEnabled())
                PowerGrid.LOGGER.debug("{}: Grabbed unloaded part of line, owner={}, between=({}, {})", this, owner, part.getNode1(), part.getNode2());
        }
    }

    public void addLastSegment(TransmissionLinePart wire) {
        assert wire.getNode1() == getNode2();
        wire.setLine(this);
        segments.add(wire);
        setResistance(resistance + wire.getResistance());
        var oldNode = getNode2();

        // Move boundary
        setNode2(wire.endpoint2, wire.getNode2());
        oldNode.remove();
        if(ENABLE_VALIDATION)
            validateLine();
    }

    public void addFirstSegment(TransmissionLinePart wire) {
        assert wire.getNode2() == getNode1();
        wire.setLine(this);
        segments.add(0, wire);
        setResistance(resistance + wire.getResistance());
        var oldNode = getNode1();

        // Move boundary
        setNode1(wire.endpoint1, wire.getNode1());
        oldNode.remove();
        if(ENABLE_VALIDATION)
            validateLine();
    }

    public void merge(TransmissionLine line) {
        assert getNode2() == line.getNode1();
        if(ModdedConfigs.logsEnabled())
            PowerGrid.LOGGER.debug("{}: Appending {}", this, line);
        segments.addAll(line.segments);
        setResistance(resistance + line.getResistance());
        var middleNode = line.getNode1();
        segments.forEach(part -> {
            if(part.owner == null || part.owner.isRemoved())
                global.bounty(part.persistentOwnerId, part.lastKnownChunk);
            part.setLine(this);
        });
        line.segments.clear();
        setNode2(line.endpoint2, line.getNode2());
        line.remove();
        middleNode.remove();
        if(ENABLE_VALIDATION)
            validateLine();
    }

    @Nullable
    public TransmissionLine splitAt(OwnedFloatingNode atNode) {
        if(atNode == node1 || atNode == node2 || endpoint1.equals(atNode.endpoint) || endpoint2.equals(atNode.endpoint))
            return null;
        if(segments.size() <= 1)
            return null;
        if(splitting) {
            PowerGrid.LOGGER.warn("Prevented a double split call", new Throwable());
            return null;
        }
        splitting = true;
        if(ModdedConfigs.logsEnabled())
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
                    global.prepareForTransmissionLine(getNode1(), splitNode, this,
                            () -> setNode2(segment.getEndpoint2(), splitNode));
                } else if(segment.getEndpoint2().equals(atNode.endpoint)) {
                    // Nodes do not match but the endpoint does. Split should occur.
                    line2 = new TransmissionLine(1, segment.endpoint2, endpoint2, global);
                    // Get the most up-to-date node.
                    var newNode = atNode.endpoint.getNode(global.world);
                    global.prepareForTransmissionLine(getNode1(), newNode, this,
                            () -> setNode2(segment.getEndpoint2(), newNode));
                }
            } else {
                R2 += segment.getResistance();
                segment.setLine(line2);
                line2.segments.add(segment);
                if(segment.owner == null || segment.owner.isRemoved())
                    global.bounty(segment.persistentOwnerId, segment.lastKnownChunk);
                iter.remove();
            }
        }
        setResistance(R1);
        if(line2 != null && R2 > 0) {
            line2.setResistance(R2);
            var network = global.prepareForConnection(line2.endpoint1, line2.endpoint2);
            if(network != null)
                network.addWire(line2);
        } else {
            splitting = false;
            throw new IllegalStateException("Splitting failed for " + this);
        }
        if(ENABLE_VALIDATION)
            validateLine();
        splitting = false;
        return line2;
    }

    public void flip() {
        var segmentsCopy = List.copyOf(segments);
        if(ModdedConfigs.logsEnabled())
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
    public void setNetwork(ElectricalNetwork network) {
        super.setNetwork(network);
        // Port pair only works for weakly coupling lines.
        if(port1 != null && network != null) {
            port1.remove();
            port2.remove();
            port1 = port2 = null;
        }
    }

    @Override
    public void setResistance(double resistance) {
        super.setResistance(resistance);
        if(port1 != null) {
            port1.setResistance((float) resistance);
            port2.setResistance((float) resistance);
        }
    }

    @Override
    public void remove() {
        super.remove();
        if(ModdedConfigs.logsEnabled())
            PowerGrid.LOGGER.debug("{}: Removing transmission line between {} and {}", this, node1, node2);
        if(port1 != null) {
            port1.remove();
            port2.remove();
            port1 = port2 = null;
        }
        for(var segment : segments) {
            global.unregisterPart(segment.persistentOwnerId, segment);
            segment.setLine(null);
        }
        optimizeNode(getNode1());
        optimizeNode(getNode2());
    }

    public void unresolve() {
        super.remove();
        if(ModdedConfigs.logsEnabled())
            PowerGrid.LOGGER.debug("{}: Unresolving transmission line between {} and {}", this, node1, node2);
        for(var segment : segments) {
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
            if(line1.getNode1() == line2.getNode2()) {
                // Append line1 onto line2
                global.globalGraph.disconnect(line1.getNode1(), line1.getNode2(), line1);
                global.prepareForTransmissionLine(line2.getNode1(), line1.getNode2(), line2, () -> line2.merge(line1));
            } else if(line1.getNode2() == line2.getNode1()) {
                // Append line2 onto line1
                global.globalGraph.disconnect(line2.getNode1(), line2.getNode2(), line2);
                global.prepareForTransmissionLine(line1.getNode1(), line2.getNode2(), line1, () -> line1.merge(line2));
            } else if(line1.getNode1() == line2.getNode1()) {
                global.globalGraph.disconnect(line2.getNode1(), line2.getNode2(), line2);
                line1.flip();
                global.prepareForTransmissionLine(line1.getNode1(), line2.getNode2(), line1, () -> line1.merge(line2));
            } else if(line1.getNode2() == line2.getNode2()) {
                global.globalGraph.disconnect(line2.getNode1(), line2.getNode2(), line2);
                line2.flip();
                global.prepareForTransmissionLine(line1.getNode1(), line2.getNode2(), line1, () -> line1.merge(line2));
            } else {
                PowerGrid.LOGGER.error("Unknown line optimization case");
            }
        }
    }

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

            var node1 = wire.endpoint2.getNode(global.world);
            var optiNode = getNode1();
            global.prepareForTransmissionLine(node1, getNode2(), this,
                    () -> setNode1(wire.endpoint2, node1));
            optimizeNode(optiNode);
            if(ENABLE_VALIDATION)
                validateLine();
        } else if (wire.getNode2() == node2) {
            // Last segment
            if(ModdedConfigs.logsEnabled())
                PowerGrid.LOGGER.debug("{}: Removing last segment of transmission line", this);
            var removed = segments.remove(segments.size() - 1);
            assert removed == wire;
            removed.setLine(null);
            if(segments.isEmpty()) {
                remove();
                return;
            }
            setResistance(resistance - removed.getResistance());

            var node2 = wire.endpoint1.getNode(global.world);
            var optiNode = getNode2();
            global.prepareForTransmissionLine(getNode1(), node2, this,
                    () -> setNode2(wire.endpoint1, node2));
            optimizeNode(optiNode);
            if(ENABLE_VALIDATION)
                validateLine();
        } else {
            // Middle segment
            if(ModdedConfigs.logsEnabled())
                PowerGrid.LOGGER.debug("{}: Removing middle segment of transmission line", this);
            var splitNode = wire.getNode2();
            var line2 = splitAt(splitNode);
            if(line2 == null)
                return;
            if(ENABLE_VALIDATION)
                line2.validateLine();

            // Last segment is the removed wire.
            var removed = segments.remove(segments.size() - 1);
            assert removed == wire;
            removed.setLine(null);
            var terminatingNode = wire.endpoint1.getNode(global.world);
            if(segments.isEmpty()) {
                if(ModdedConfigs.logsEnabled())
                    PowerGrid.LOGGER.debug("{}: Split and remove resulted in an empty line", this);
                remove();
                return;
            }
            setResistance(resistance - removed.getResistance());
            global.prepareForTransmissionLine(getNode1(), terminatingNode, this,
                    () -> setNode2(wire.endpoint1, terminatingNode));
            if(ENABLE_VALIDATION)
                validateLine();
        }
    }

    public void makePortPair() {
        if(node1.getNetwork() == node2.getNetwork() || network != null)
            return;
        if(port1 != null) {
            port1.remove();
            port2.remove();
        }
        port1 = new TransmissionLinePort(node1, (float) resistance, this);
        port2 = new TransmissionLinePort(node2, (float) resistance, this);
        port1.other = port2;
        port2.other = port1;
        var Isum = potentialDifference() * conductance();
        port1.I = -(float) (Isum * 0.5f);
        port2.I = -(float) (Isum * 0.5f);
        node1.getNetwork().addNode(port1);
        node2.getNetwork().addNode(port2);
    }

    @Override
    public String toString() {
        return String.format("TransmissionLine[id=%d]", id);
    }

    public int getId() {
        return id;
    }

    public float voltageFor(OwnedFloatingNode node) {
        if(node == node1 || node == node2)
            return node.getVoltage();
        float R = 0;
        for(var segment : segments) {
            R += segment.getResistance();
            if(segment.getNode2() == node) {
                var a = (float) (R / getResistance());
                return node1.getVoltage() * (1 - a) + node2.getVoltage() * a;
            }
        }
        return node2.getVoltage();
    }

    public void tick() {
        if(port1 == null)
            return;
        if(node1.getNetwork() == node2.getNetwork()) {
            port1.remove();
            port2.remove();
            port1 = port2 = null;
            node1.getNetwork().addWire(this);
        } else if(getNetwork() == null && !WorldNetworks.canWeakCouple(this)) {
            node1.getNetwork().merge(node2.getNetwork());
            unifyPorts(node1.getNetwork());
        }
    }

    public static void unifyPorts(ElectricalNetwork network) {
        var lines = new HashSet<TransmissionLine>();
        for(var node : network.getNodes()) {
            if(!(node instanceof TransmissionLinePort port))
                continue;
            if(port.getNetwork() == port.getOther().getNetwork() && port.getNetwork() == network)
                lines.add(port.getLine());
        }
        lines.forEach(network::addWire);
    }
}
