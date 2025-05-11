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
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GlobalElectricNetworks {
    protected static final Map<World, List<ElectricalNetwork>> worldNetworks = new HashMap<>();

    public static void init() {
        ServerTickEvents.START_WORLD_TICK.register(GlobalElectricNetworks::tick);
        ServerWorldEvents.UNLOAD.register((server, world) -> worldNetworks.remove(world));
    }

    protected static void tick(World world) {
        var networks = worldNetworks.get(world);
        if(networks == null)
            return;
        List<ElectricalNetwork> removed = new LinkedList<>();
        for(final var network : networks) {
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
    }

    public static ElectricalNetwork createNetwork(World level) {
        var network = new ElectricalNetwork();
        var networkList = worldNetworks.computeIfAbsent(level, key -> new LinkedList<>());
        networkList.add(network);
        return network;
    }

    public static ElectricWire makeConnection(ElectricBehaviour behaviour1, IElectricNode node1, ElectricBehaviour behaviour2, IElectricNode node2, float resistance) {
        assert behaviour1.getWorld() == behaviour2.getWorld();
        if(node1 == node2)
            return null;

        // Put both nodes into the same network.
        ElectricalNetwork network;
        if(node1.getNetwork() == null && node2.getNetwork() == null) {
            network = GlobalElectricNetworks.createNetwork(behaviour1.getWorld());
            behaviour1.joinNetwork(network);
            if(behaviour1 != behaviour2)
                behaviour2.joinNetwork(network);
        } else if(node1.getNetwork() == null) {
            network = node2.getNetwork();
            behaviour1.joinNetwork(network);
        } else if(node2.getNetwork() == null) {
            network = node1.getNetwork();
            behaviour2.joinNetwork(network);
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

        var wire = new ElectricWire(resistance, node1, node2);
        network.addWire(wire);
        return wire;
    }
}
