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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.electricity.sim.*;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.network.packets.TransmissionLineS2CPacket;
import org.patryk3211.powergrid.utility.PlayerUtilities;

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
        var iter = networks.subnetworks.iterator();
        while(iter.hasNext()) {
            var network = iter.next();
            if(network.isEmpty()) {
                iter.remove();
                continue;
            }
            if(network.isDirty()) {
                // Two more recalculations to make sure the network is stable.
                network.calculate();
                network.calculate();
            }
            network.calculate();
        }
        if(world instanceof ServerWorld serverWorld) {
            for(var line : networks.transmissionLines) {
                var players = PlayerUtilities.partialTracking(serverWorld, line);
                if(players.isEmpty())
                    continue;
                var packet = new TransmissionLineS2CPacket(line);
                ModdedPackets.getChannel().sendToClients(packet, players);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static WorldNetworks makeClientWorldNetworks(World world) {
        return new ClientWorldNetworks(world);
    }

    public static WorldNetworks getWorldNetworks(World world) {
        return worldNetworks.computeIfAbsent(world, key -> {
            if(key.isClient)
                return makeClientWorldNetworks(key);
            return new WorldNetworks(key);
        });
    }

    public static TransmissionLine getLine(WireEntity entity) {
        var wire = entity.getWire();
        if(wire == null)
            return null;
        var worldNetworks = getWorldNetworks(entity.getWorld());
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

    public static ElectricWire makeConnection(World world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        return getWorldNetworks(world).makeTransmissionLine(endpoint1, endpoint2, forEntity);
    }

    public static void inspect(IElectricNode node, PlayerEntity user) {
        var worldNetworks = getWorldNetworks(user.getWorld());
        user.sendMessage(Text.of(user instanceof ServerPlayerEntity ? "Server:" : "Client:"));
        user.sendMessage(Text.literal(node.toString()));
        for(var connected : worldNetworks.globalGraph.getConnectedNodes(node)) {
            user.sendMessage(Text.literal(" - " + connected));
            for(var wire : worldNetworks.globalGraph.getWires(node, connected)) {
                user.sendMessage(Text.literal("  via " + wire));
            }
        }
    }
}
