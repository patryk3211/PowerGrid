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

import net.createmod.ponder.api.level.PonderLevel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.utility.NumberFormats;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalElectricNetworks {
    protected static final Map<Level, WorldNetworks> worldNetworks = new ConcurrentHashMap<>();

    public static void preTick(Level world) {
        var networks = worldNetworks.get(world);
        if(networks == null)
            return;
        networks.preTick();
    }

    public static void postTick(Level world) {
        var networks = worldNetworks.get(world);
        if(networks == null)
            return;
        networks.postTick();
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
            if(key instanceof PonderLevel)
                return new WorldNetworks(key);
            if(key.isClientSide) return makeClientWorldNetworks(key);
            if(world instanceof ServerLevel server) {
                return server.getDataStorage().computeIfAbsent(
                        nbt -> new WorldNetworks(world, nbt),
                        () -> new WorldNetworks(world),
                        "powergrid_electric_network_data"
                );
            } else {
                return new WorldNetworks(world);
            }
        });
    }

    @Nullable
    public static WorldNetworks getWorldNetworks(LevelAccessor world) {
        return worldNetworks.get(world);
    }

    public static TransmissionLine getLine(WireEntity entity) {
        var wire = entity.getWire();
        if(wire == null)
            return null;
        var worldNetworks = getWorldNetworks(entity.level());
        if(wire.getNode1() instanceof OwnedFloatingNode owned) {
            var line = worldNetworks.findLineMiddle(owned);
            if (line != null && line.isPart(wire)) {
                return line;
            }
        }
        if(wire.getNode2() instanceof OwnedFloatingNode owned) {
            var line = worldNetworks.findLineMiddle(owned);
            if (line != null && line.isPart(wire)) {
                return line;
            }
        }
        // If that fails, the only other option is that the line has one segment (or doesn't exist).
        var lineWire = worldNetworks.globalGraph.getFirstWire(wire.getNode1(), wire.getNode2());
        if(lineWire instanceof TransmissionLine line1) {
            return line1;
        }
        return null;
    }

    public static ElectricWire makeConnection(Level world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, BaseWireEntity forEntity, WorldNetworks.PartId id) {
        return getWorldNetworks(world).makeTransmissionLine(endpoint1, endpoint2, forEntity, id);
    }

    public static ElectricWire makeSimpleConnection(Level world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, float resistance) {
        return getWorldNetworks(world).makeSimpleWire(endpoint1, endpoint2, resistance);
    }

    private static Component display(IElectricNode node) {
        MutableComponent line;
        if(node instanceof OwnedFloatingNode ofn) {
            line = Component.literal("OFN[" + ofn.endpoint + "]");
        } else {
            line = Component.literal(node.toString());
        }
        line.append(Component.literal("@" + NumberFormats.formatPrecise(node.getVoltage()) + "V").withStyle(ChatFormatting.DARK_GRAY));
        if(node.getNetwork() != null) {
            if(node.getNetwork().isLeaf(node)) {
                line.append(Component.literal(" -").withStyle(ChatFormatting.GREEN));
            } else if(node.getNetwork().isOptimized(node)) {
                line.append(Component.literal(" *").withStyle(ChatFormatting.GREEN));
            }
        }
        return line;
    }

    public static void inspect(ElectricBehaviour behaviour, Player user) {
        var worldNetworks = getWorldNetworks(user.level());
        user.sendSystemMessage(user instanceof ServerPlayer
                ? Component.literal("Server:").withStyle(ChatFormatting.GOLD)
                : Component.literal("Client:").withStyle(ChatFormatting.GREEN));
        int index = 0;
        for(var node : behaviour.getExternalNodes()) {
            user.sendSystemMessage(Component.literal((index++) + " = ").append(display(node))
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            for (var connected : worldNetworks.globalGraph.getConnectedNodes(node)) {
                user.sendSystemMessage(Component.literal(" - ").append(display(connected))
                        .withStyle(ChatFormatting.BLUE));
                for (var wire : worldNetworks.globalGraph.getWires(node, connected)) {
                    user.sendSystemMessage(Component.literal("    via " + wire)
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
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
        for(OwnedFloatingNode node : behaviour.getExternalNodes()) {
            worldNetworks.nodeHolderAdded(node, behaviour.hasInternals());
        }
    }

    public static void prepareUnpaused(ElectricBehaviour behaviour) {
        var worldNetworks = getWorldNetworks(behaviour.blockEntity.getLevel());
        for(OwnedFloatingNode node : behaviour.getExternalNodes()) {
            worldNetworks.prepareUnpaused(node);
        }
    }

    public static void configsReloaded() {
        var solverType = ModdedConfigs.server().electricity.solver.solverType.get();
        var rA = ModdedConfigs.server().electricity.solver.solverAbsolutePrecision.get();
        var rR = ModdedConfigs.server().electricity.solver.solverRelativePrecision.get();
        var rM = ModdedConfigs.server().electricity.solver.solverAbsoluteMinimumPrecision.get();
        for(var networks : worldNetworks.values()) {
            networks.subnetworks.forEach(network -> {
                network.setSolverType(solverType);
                network.setPrecision(rA, rR, rM);
            });
        }
    }

}
