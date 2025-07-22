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
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransmissionLine extends ElectricWire {
    public final List<TransmissionLinePart> segments = new ArrayList<>();

    private final GlobalElectricNetworks.WorldNetworks global;

    private TransmissionLine(double resistance, IElectricNode node1, IElectricNode node2, GlobalElectricNetworks.WorldNetworks global) {
        super(resistance, node1, node2);
        this.global = global;
    }

    public TransmissionLine(double resistance, IElectricNode node1, IElectricNode node2, TransmissionLinePart firstSegment, GlobalElectricNetworks.WorldNetworks global) {
        super(resistance, node1, node2);
        segments.add(firstSegment);
        this.global = global;
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

    public void removeSegment(ElectricWire wire) {
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
        return String.format("TransmissionLine@%s", Integer.toHexString(hashCode()));
    }

    // Prevent unnecessary electrical network updates.
    private NetworkFreeze freeze() {
        return new NetworkFreeze(this);
    }

    private static class NetworkFreeze implements AutoCloseable {
        private final TransmissionLine line;
        private final ElectricalNetwork network;

        public NetworkFreeze(TransmissionLine line) {
            this.line = line;
            network = line.getNetwork();
            if(network != null) {
                network.removeWire(line);
                line.setNetwork(null);
            }
        }

        @Override
        public void close() {
            if(network != null) {
                if(line.node1.getNetwork() == null || line.node2.getNetwork() == null) {
                    PowerGrid.LOGGER.error("Transmission line terminating node is not in a network. How did this happen?");
                    return;
                }
                network.addWire(line);
            }
        }

        public void addNode(IElectricNode node) {
            if(network != null)
                network.addNode(node);
        }
    }

//    private static class InnerNode implements IElectricNode {
//        private final TransmissionLine owner;
//        private final double potentialRatio;
//        private final IElectricNode originalNode;
//
//        public InnerNode(TransmissionLine owner, double potentialRatio, IElectricNode originalNode) {
//            this.owner = owner;
//            this.potentialRatio = potentialRatio;
//            if(originalNode instanceof InnerNode inner)
//                originalNode = inner.originalNode;
//            this.originalNode = originalNode;
//        }
//
//        @Override
//        public float getVoltage() {
//            return (float) (owner.node1.getVoltage() * (1 - potentialRatio) + owner.node2.getVoltage() * potentialRatio);
//        }
//
//        @Override
//        public float getCurrent() {
//            return 0;
//        }
//
//        @Override
//        public void receiveResult(float value) { }
//
//        @Override
//        public void assignIndex(int index) { }
//
//        @Override
//        public int getIndex() {
//            return -1;
//        }
//        @Override
//        public void setNetwork(ElectricalNetwork network) {
//            if(network != owner.network) {
//                throw new IllegalArgumentException("Cannot add to a network other than the owner's");
//            }
//        }
//
//        @Override
//        public ElectricalNetwork getNetwork() {
//            return owner.network;
//        }
//    }
}
