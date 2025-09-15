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
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.Objects;
import java.util.UUID;

public class TransmissionLinePart extends ElectricWire {
    @Nullable
    private TransmissionLine line;
    @NotNull
    private final WorldNetworks global;

    @Nullable
    public WireEntity owner;
    public final UUID persistentOwnerId;
    public ChunkPos lastKnownChunk;

    @NotNull
    public IWireEndpoint endpoint1;
    @NotNull
    public IWireEndpoint endpoint2;

    private TransmissionLinePart(double resistance, @NotNull IWireEndpoint endpoint1, @NotNull IWireEndpoint endpoint2, UUID ownerId, ChunkPos lastKnownChunk, @NotNull WorldNetworks global) {
        super(resistance, global.holderOrPlaceholderNode(endpoint1), global.holderOrPlaceholderNode(endpoint2));
        this.global = global;
        this.endpoint1 = endpoint1;
        this.endpoint2 = endpoint2;
        this.persistentOwnerId = ownerId;
        this.lastKnownChunk = lastKnownChunk;
        global.registerPart(persistentOwnerId, this);
    }

    private TransmissionLinePart(double resistance, @NotNull IWireEndpoint endpoint1, @NotNull IWireEndpoint endpoint2, @NotNull WireEntity owner, @NotNull WorldNetworks global) {
        super(resistance, global.holderOrPlaceholderNode(endpoint1), global.holderOrPlaceholderNode(endpoint2));
        this.global = global;
        this.owner = owner;
        this.endpoint1 = endpoint1;
        this.endpoint2 = endpoint2;
        this.persistentOwnerId = owner.getUUID();
        this.lastKnownChunk = new ChunkPos(owner.blockPosition());
        global.registerPart(persistentOwnerId, this);
    }

    private TransmissionLinePart(double resistance, @NotNull IWireEndpoint endpoint1, @NotNull IWireEndpoint endpoint2, @NotNull WireEntity owner, @NotNull TransmissionLine line) {
        super(resistance, line.global().holderOrPlaceholderNode(endpoint1), line.global().holderOrPlaceholderNode(endpoint2));
        this.line = line;
        this.global = line.global();
        this.owner = owner;
        this.endpoint1 = endpoint1;
        this.endpoint2 = endpoint2;
        this.persistentOwnerId = owner.getUUID();
        this.lastKnownChunk = new ChunkPos(owner.blockPosition());
        global.registerPart(persistentOwnerId, this);
    }

    private TransmissionLinePart(double resistance, @NotNull IWireEndpoint endpoint1, @NotNull IWireEndpoint endpoint2, UUID ownerId, ChunkPos lastKnownChunk, @NotNull TransmissionLine line) {
        super(resistance, line.global().holderOrPlaceholderNode(endpoint1), line.global().holderOrPlaceholderNode(endpoint2));
        this.global = line.global();
        this.persistentOwnerId = ownerId;
        this.lastKnownChunk = lastKnownChunk;
        this.endpoint1 = endpoint1;
        this.endpoint2 = endpoint2;
        this.line = line;
        global.registerPart(persistentOwnerId, this);
    }

    public static TransmissionLinePart uniquePart(CompoundTag tag, WorldNetworks global) {
        var ownerId = tag.getUUID("Owner");
        var resistance = tag.getDouble("Resistance");
        var endpoint1 = WireEndpointType.deserialize(tag.getCompound("Node1"));
        var endpoint2 = WireEndpointType.deserialize(tag.getCompound("Node2"));
        var lastKnownChunk = new ChunkPos(tag.getInt("X"), tag.getInt("Z"));
        var part = global.getPart(ownerId);
        if(part == null)
            return new TransmissionLinePart(resistance, endpoint1, endpoint2, ownerId, lastKnownChunk, global);
        if(!endpoint1.equals(part.endpoint1))
            throw new IllegalStateException();
        if(!endpoint2.equals(part.endpoint2))
            throw new IllegalStateException();
        return part;
    }

    public static TransmissionLinePart uniquePart(double resistance, @NotNull IWireEndpoint endpoint1, @NotNull IWireEndpoint endpoint2, WireEntity owner, @NotNull WorldNetworks global) {
        var part = global.getPart(owner.getUUID());
        if(part == null)
            return new TransmissionLinePart(resistance, endpoint1, endpoint2, owner, global);
        if(!endpoint1.equals(part.endpoint1))
            throw new IllegalStateException();
        if(!endpoint2.equals(part.endpoint2))
            throw new IllegalStateException();
        return part;
    }

    public static TransmissionLinePart uniquePart(double resistance, @NotNull IWireEndpoint endpoint1, @NotNull IWireEndpoint endpoint2, UUID ownerId, ChunkPos lastKnownChunk, @NotNull TransmissionLine line) {
        var part = line.global().getPart(ownerId);
        if(part == null)
            return new TransmissionLinePart(resistance, endpoint1, endpoint2, ownerId, lastKnownChunk, line);
        if(!endpoint1.equals(part.endpoint1))
            throw new IllegalStateException();
        if(!endpoint2.equals(part.endpoint2))
            throw new IllegalStateException();
        return part;
    }

    public static TransmissionLinePart uniquePart(double resistance, @NotNull IWireEndpoint endpoint1, @NotNull IWireEndpoint endpoint2, @NotNull WireEntity owner, @NotNull TransmissionLine line) {
        var part = line.global().getPart(owner.getUUID());
        if(part == null)
            return new TransmissionLinePart(resistance, endpoint1, endpoint2, owner, line);
        if(!endpoint1.equals(part.endpoint1))
            throw new IllegalStateException();
        if(!endpoint2.equals(part.endpoint2))
            throw new IllegalStateException();
        return part;
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

    @Nullable
    public TransmissionLine getLine() {
        return line;
    }

    public void setLine(@Nullable TransmissionLine line) {
        this.line = line;
    }

    public void unload() {
        assert owner != null : "Node already unloaded";
        if(ModdedConfigs.logsEnabled())
            PowerGrid.LOGGER.debug("{}: Unloading part, UUID={}, chunk={}", line, persistentOwnerId, lastKnownChunk);
        lastKnownChunk = new ChunkPos(owner.blockPosition());
        global.bounty(persistentOwnerId, lastKnownChunk);
        owner = null;
    }

    public void grab(WireEntity forEntity) {
        if(persistentOwnerId.equals(forEntity.getUUID())) {
            owner = forEntity;
            if (line != null)
                line.grabPart(forEntity, this);
        } else {
            PowerGrid.LOGGER.warn("Entity tried to grab a part which it does not own, part: {}, entity: {}", this, forEntity);
        }
    }

    // Transmission line part can NEVER be directly in a network.
    @Override
    public void setNetwork(ElectricalNetwork network) {
        throw new IllegalCallerException();
    }

    @Override
    public void remove() {
        if(ModdedConfigs.logsEnabled())
            PowerGrid.LOGGER.debug("Removing {}", this);
        if(line != null)
            line.remove(this);
        global.unregisterPart(persistentOwnerId, this);
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

    @Override
    public String toString() {
        return String.format("LinePart[id=%s, %s, %s]", persistentOwnerId, endpoint1, endpoint2);
    }

    public CompoundTag toNbt() {
        var tag = new CompoundTag();
        tag.put("Node1", endpoint1.serialize());
        tag.put("Node2", endpoint2.serialize());
        tag.putUUID("Owner", persistentOwnerId);
        if(owner != null)
            lastKnownChunk = new ChunkPos(owner.blockPosition());
        tag.putInt("X", lastKnownChunk.x);
        tag.putInt("Z", lastKnownChunk.z);
        tag.putDouble("Resistance", resistance);
        return tag;
    }

    public void refreshEndpointNodes() {
        var node1 = endpoint1.getNode(global.world);
        if(this.node1 != node1) {
            global.addAndMigrateNode(getNode1().endpoint, node1);
            setNode1(node1);
        }
        var node2 = endpoint2.getNode(global.world);
        if(this.node2 != node2) {
            global.addAndMigrateNode(getNode2().endpoint, node2);
            setNode2(node2);
        }
    }
}
