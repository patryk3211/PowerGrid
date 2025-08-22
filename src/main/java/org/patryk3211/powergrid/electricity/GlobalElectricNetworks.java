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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.HashMap;
import java.util.Map;

public class GlobalElectricNetworks {
    protected static final Map<Level, WorldNetworks> worldNetworks = new HashMap<>();

    public static void tick(Level world) {
        var networks = worldNetworks.get(world);
        if(networks == null)
            return;
        networks.tick();
    }

    public static void unloadWorld(ServerLevel world) {
        worldNetworks.remove(world);
    }

    @Environment(EnvType.CLIENT)
    public static WorldNetworks makeClientWorldNetworks(Level world) {
        return new ClientWorldNetworks(world);
    }

    public static WorldNetworks getWorldNetworks(Level world) {
        return worldNetworks.computeIfAbsent(world, key -> {
            if(key.isClientSide) return makeClientWorldNetworks(key);
            var server = (ServerLevel) world;
            return server.getDataStorage().computeIfAbsent(
                    nbt -> new WorldNetworks(world, nbt),
                    () -> new WorldNetworks(world),
                    "powergrid_electric_network_data"
            );
        });
    }

    public static TransmissionLine getLine(WireEntity entity) {
        var wire = entity.getWire();
        if(wire == null)
            return null;
        var worldNetworks = getWorldNetworks(entity.level());
        var line = worldNetworks.transmissionLineNodes.get(wire.getNode1());
        if(line != null && line.isPart(wire)) {
            return line;
        }
        line = worldNetworks.transmissionLineNodes.get(wire.getNode2());
        if(line != null && line.isPart(wire)) {
            return line;
        }
        // If that fails, the only other option is that the line has one segment (or doesn't exist).
        var lineWire = worldNetworks.globalGraph.getFirstWire(wire.getNode1(), wire.getNode2());
        if(lineWire instanceof TransmissionLine line1) {
            return line1;
        }
        return null;
    }

    public static ElectricWire makeConnection(Level world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        return getWorldNetworks(world).makeTransmissionLine(endpoint1, endpoint2, forEntity);
    }

    public static void inspect(IElectricNode node, Player user) {
        var worldNetworks = getWorldNetworks(user.level());
        user.sendSystemMessage(Component.nullToEmpty(user instanceof ServerPlayer ? "Server:" : "Client:"));
        user.sendSystemMessage(Component.literal(node.toString()));
        for(var connected : worldNetworks.globalGraph.getConnectedNodes(node)) {
            user.sendSystemMessage(Component.literal(" - " + connected));
            for(var wire : worldNetworks.globalGraph.getWires(node, connected)) {
                user.sendSystemMessage(Component.literal("  via " + wire));
            }
        }
    }

    // This function should handle unloading unneeded transmission lines and removal of electric nodes.
    public static void nodeHolderUnloaded(ElectricBehaviour behaviour) {
        var worldNetworks = getWorldNetworks(behaviour.blockEntity.getLevel());
        for(IElectricNode node : behaviour.getExternalNodes()) {
            if(node instanceof OwnedFloatingNode ownedNode)
                worldNetworks.nodeHolderUnloaded(ownedNode);
        }
    }

    public static void nodeHolderRemoved(ElectricBehaviour behaviour) {
        var worldNetworks = getWorldNetworks(behaviour.blockEntity.getLevel());
        for(IElectricNode node : behaviour.getExternalNodes()) {
            if(node instanceof OwnedFloatingNode ownedNode)
                worldNetworks.nodeHolderRemoved(ownedNode);
        }
    }

    public static void nodeHolderAdded(ElectricBehaviour behaviour) {
        var worldNetworks = getWorldNetworks(behaviour.blockEntity.getLevel());
        for(IElectricNode node : behaviour.getExternalNodes()) {
            if(node instanceof OwnedFloatingNode ownedNode)
                worldNetworks.nodeHolderAdded(ownedNode);
        }
    }
}
