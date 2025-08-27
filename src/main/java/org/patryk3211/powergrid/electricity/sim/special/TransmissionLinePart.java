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

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.Objects;
import java.util.UUID;

public class TransmissionLinePart extends ElectricWire {
    private TransmissionLine line;
    @Nullable
    public WireEntity owner;
    public final UUID persistentOwnerId;
    public ChunkPos lastKnownChunk;

    @NotNull
    public IWireEndpoint endpoint1;
    @NotNull
    public IWireEndpoint endpoint2;

    public TransmissionLinePart(double resistance, @NotNull IWireEndpoint endpoint1, @NotNull IWireEndpoint endpoint2, Level level, @NotNull WireEntity owner, TransmissionLine line) {
        super(resistance, endpoint1.getNode(level), endpoint2.getNode(level));
        this.line = line;
        this.owner = owner;
        this.endpoint1 = endpoint1;
        this.endpoint2 = endpoint2;
        this.persistentOwnerId = owner.getUUID();
        this.lastKnownChunk = new ChunkPos(owner.blockPosition());
    }

    public TransmissionLinePart(double resistance, @NotNull IWireEndpoint endpoint1, @NotNull IWireEndpoint endpoint2, UUID ownerId, ChunkPos lastKnownChunk, TransmissionLine line) {
        super(resistance, null, null);
        this.persistentOwnerId = ownerId;
        this.lastKnownChunk = lastKnownChunk;
        this.endpoint1 = endpoint1;
        this.endpoint2 = endpoint2;
        this.line = line;
    }

    public void setNode1(IWireEndpoint endpoint, OwnedFloatingNode node) {
        this.endpoint1 = endpoint;
        super.setNode1(node);
    }

    public void setNode2(IWireEndpoint endpoint, OwnedFloatingNode node) {
        this.endpoint2 = endpoint;
        super.setNode2(node);
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

    @NotNull
    public IWireEndpoint getEndpoint1() {
        return endpoint1;
    }

    @NotNull
    public IWireEndpoint getEndpoint2() {
        return endpoint2;
    }

    public TransmissionLine getLine() {
        return line;
    }

    public void setLine(TransmissionLine line) {
        this.line = line;
    }

    public void unload() {
        if(line != null)
            line.unloadPart(this);
    }

    // Transmission line part can NEVER be directly in a network.
    @Override
    public void setNetwork(ElectricalNetwork network) {
        throw new IllegalCallerException();
    }

    @Override
    public void remove() {
        if(line != null)
            line.removeSegment(this);
    }

    @Override
    public float potentialDifference() {
        if(line == null)
            return 0;
        return (float) (line.current() * getResistance());
    }

    @Override
    public float current() {
        if(line == null)
            return 0;
        return line.current();
    }
}
