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
package org.patryk3211.powergrid.electricity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.sim.*;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.*;

public class GlobalElectricNetworks {
    protected static final Map<World, WorldNetworks> worldNetworks = new HashMap<>();

    public static void init() {
        ServerTickEvents.START_WORLD_TICK.register(GlobalElectricNetworks::tick);
        ServerWorldEvents.UNLOAD.register((server, world) -> worldNetworks.remove(world));
    }

    protected static void tick(World world) {
        var networks = worldNetworks.get(world);
        if(networks == null)
            return;
        List<ElectricalNetwork> removed = new LinkedList<>();
        for(final var network : networks.subnetworks) {
            if(network.isEmpty()) {
                removed.add(network);
                continue;
            }
            if(network.isDirty()) {
                // Two more recalculations to make sure the network is stable.
                network.calculate();
                network.calculate();
            }
            network.calculate();
        }
        networks.removeAll(removed);
        for(final var network : networks.subnetworks) {
            network.calculate();
        }
    }

    public static WorldNetworks getWorldNetworks(World world) {
        return worldNetworks.computeIfAbsent(world, WorldNetworks::new);
    }

    public static ElectricalNetwork createNetwork(World world) {
        var networkList = getWorldNetworks(world);
        return networkList.newNetwork();
    }

    public static ElectricWire makeSimpleWire(World world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        if(node1 == node2)
            return null;
        if(node1 == null || node2 == null)
            return null;

        // Put both nodes into the same network.
        ElectricalNetwork network;
        if(node1.getNetwork() == null && node2.getNetwork() == null) {
            network = GlobalElectricNetworks.createNetwork(world);
            endpoint1.joinNetwork(world, network);
            endpoint2.joinNetwork(world, network);
        } else if(node1.getNetwork() == null) {
            network = node2.getNetwork();
            endpoint1.joinNetwork(world, network);
        } else if(node2.getNetwork() == null) {
            network = node1.getNetwork();
            endpoint2.joinNetwork(world, network);
        } else if(node1.getNetwork() != node2.getNetwork()) {
            if(node1.getNetwork().size() >= node2.getNetwork().size()) {
                network = node1.getNetwork();
                network.merge(node2.getNetwork());
            } else {
                network = node2.getNetwork();
                network.merge(node1.getNetwork());
            }
        } else {
            network = node1.getNetwork();
        }

        var wire = new OwnedElectricWire(forEntity.getResistance(), node1, node2, forEntity);
        network.addWire(wire);

        var worldNetworks = getWorldNetworks(world);
        worldNetworks.globalGraph.connect(node1, node2, forEntity, wire);
        return wire;
    }

    public static ElectricWire makeConnection(World world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        if(node1 == node2)
            return null;
        if(node1 == null || node2 == null)
            return null;

        var worldNetworks = getWorldNetworks(world);
        worldNetworks.add(endpoint1);
        worldNetworks.add(endpoint2);

        return makeSimpleWire(world, endpoint1, endpoint2, forEntity);
    }

    public static class WorldNetworks {
        public final World world;
        public final List<ElectricalNetwork> subnetworks = new ArrayList<>();
        public final NetworkGraph globalGraph = new NetworkGraph();

        public WorldNetworks(World world) {
            this.world = world;
        }

        public ElectricalNetwork newNetwork() {
            var network = new GraphedElectricalNetwork(globalGraph);
            subnetworks.add(network);
            return network;
        }

        public void add(ElectricalNetwork network) {
            subnetworks.add(network);
        }

        public void add(IWireEndpoint endpoint) {
            if(endpoint instanceof BlockWireEndpoint blockEndpoint) {
                var block = world.getBlockState(blockEndpoint.getPos());
                globalGraph.addNode(endpoint.getNode(world));
            }
        }

        public int connectionCount(IWireEndpoint endpoint) {
            return globalGraph.connectionCount(endpoint.getNode(world));
        }

        public void removeAll(Collection<ElectricalNetwork> networks) {
            subnetworks.removeAll(networks);
        }
    }
}
