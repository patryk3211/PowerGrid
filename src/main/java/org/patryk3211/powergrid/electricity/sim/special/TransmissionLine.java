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
        segments.addAll(line.segments);
        setResistance(resistance + line.getResistance());
        global.assignTransmissionLine(line.getNode1(), this);
        segments.forEach(part -> {
            global.assignTransmissionLine(part.getNode2(), this);
            part.setLine(this);
        });
        global.assignTransmissionLine(line.getNode2(), null);
        line.segments.clear();
        line.remove();
        setNode2(line.getNode2());
    }

    @Nullable
    public TransmissionLine splitAt(IElectricNode atNode) {
        if(atNode == node1 || atNode == node2)
            return null;
        if(segments.size() == 1)
            return null;
        PowerGrid.LOGGER.trace("Splitting transmission line between {} and {} at {}", node1, node2, atNode);
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
            PowerGrid.LOGGER.trace("  Splitting failed");
            global.assignTransmissionLine(getNode2(), null);
        }
        return line2;
    }

    public void flip() {
        var segmentsCopy = List.copyOf(segments);
        PowerGrid.LOGGER.trace("Flipping transmission line between {} and {}", node1, node2);
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
        PowerGrid.LOGGER.trace("Removing transmission line between {} and {}", node1, node2);
        for(var segment : segments) {
            global.assignTransmissionLine(segment.getNode2(), null);
        }
    }

    public boolean isPart(ElectricWire wire) {
        return segments.contains(wire);
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
            PowerGrid.LOGGER.trace("Removing first segment of transmission line");
            var removed = segments.remove(0);
            if(segments.isEmpty()) {
                remove();
                return;
            }
            assert removed == wire;
            removed.setLine(null);
            setResistance(resistance - removed.getResistance());

            var node1 = wire.getNode2();
            global.assignTransmissionLine(node1, null);
            if(network != null)
                network.addNode(node1);
            setNode1(node1);
        } else if (wire.getNode2() == node2) {
            // Last segment
            PowerGrid.LOGGER.trace("Removing last segment of transmission line");
            var removed = segments.remove(segments.size() - 1);
            if(segments.isEmpty()) {
                remove();
                return;
            }
            assert removed == wire;
            removed.setLine(null);
            setResistance(resistance - removed.getResistance());

            var node2 = wire.getNode1();
            global.assignTransmissionLine(node2, null);
            if(network != null)
                network.addNode(node2);
            setNode2(node2);
        } else {
            // Middle segment
            PowerGrid.LOGGER.trace("Removing middle segment of transmission line");
            var splitNode = wire.getNode2();
            var line2 = splitAt(splitNode);
            if(line2 == null)
                return;

            global.assignTransmissionLine(splitNode, null);
            if(network != null)
                network.addNode(splitNode);

            // Last segment is the removed wire.
            var removed = segments.remove(segments.size() - 1);
            removed.setLine(null);
            if(segments.isEmpty()) {
                PowerGrid.LOGGER.trace("Split and remove resulted in an empty line");
                remove();
                return;
            }
            assert removed == wire;
            setResistance(resistance - removed.getResistance());
            var terminatingNode = wire.getNode1();
            global.assignTransmissionLine(terminatingNode, null);
            if(network != null)
                network.addNode(terminatingNode);
            setNode2(terminatingNode);
        }
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
