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
package org.patryk3211.powergrid.collections;

import dev.architectury.networking.NetworkChannel;
import dev.architectury.networking.NetworkManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.zapper.ElectroZapperS2CPacket;
import org.patryk3211.powergrid.network.SimplePacket;
import org.patryk3211.powergrid.network.packets.*;
import org.patryk3211.powergrid.utility.PlayerLookup;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public enum ModdedPackets {
    ELECTRO_ZAPPER_SHOOT(ElectroZapperS2CPacket.class, ElectroZapperS2CPacket::new),
    ZAP_PROJECTILE(ZapProjectileS2CPacket.class, ZapProjectileS2CPacket::new),
    SOLVER_SYNC(SolverStateS2CPacket.class, SolverStateS2CPacket::new),
    TRANSMISSION_LINE(TransmissionLineS2CPacket.class, TransmissionLineS2CPacket::new),
    LIGHTNING_SYNC(LightningSyncS2CPacket.class, LightningSyncS2CPacket::new),
    ENTITY_DATA(EntityDataS2CPacket.class, EntityDataS2CPacket::new),

    TRANSFORMER_WINDING(TransformerWindingC2SPacket.class, TransformerWindingC2SPacket::new),
    CHANGE_SCREEN(ChangeScreenC2SPacket.class, ChangeScreenC2SPacket::new),
    SAVE_SCHEMATIC(SaveSchematicC2SPacket.class, SaveSchematicC2SPacket::new),
    BLOCK_WIRE_CUT(BlockWireCutC2SPacket.class, BlockWireCutC2SPacket::new),
    BLOCK_WIRE_ATTACH(BlockWireAttachC2SPacket.class, BlockWireAttachC2SPacket::new),

    UPDATE_COMPONENT(UpdateComponentBiPacket.class, UpdateComponentBiPacket::new),
    ;

    public static final Identifier CHANNEL_NAME = PowerGrid.asResource("main");
    private static NetworkChannel channel;

    private final PacketType<?> type;

    <T extends SimplePacket> ModdedPackets(Class<T> type, Function<PacketByteBuf, T> factory) {
        this.type = new PacketType<>(type, SimplePacket::encode, factory, SimplePacket::handle);
    }

    public static void registerPackets() {
        channel = NetworkChannel.create(CHANNEL_NAME);
        for(var packet : values())
            packet.type.register();
    }

    public static NetworkChannel getChannel() {
        return channel;
    }

    public static class PacketType<T> {
        private final BiConsumer<T, PacketByteBuf> encoder;
        private final Function<PacketByteBuf, T> decoder;
        private final BiConsumer<T, Supplier<NetworkManager.PacketContext>> handler;
        private final Class<T> type;

        private PacketType(Class<T> type, BiConsumer<T, PacketByteBuf> encoder, Function<PacketByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkManager.PacketContext>> handler) {
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
            this.type = type;
        }

        private void register() {
            getChannel().register(type, encoder, decoder, handler);
        }
    }

    public static <T> void sendToServer(T packet) {
        channel.sendToServer(packet);
    }

    public static <T> void sendToClient(T packet, ServerPlayerEntity player) {
        channel.sendToPlayer(player, packet);
    }

    public static <T> void sendToClients(T packet, Iterable<ServerPlayerEntity> players) {
        channel.sendToPlayers(players, packet);
    }

    public static <T> void sendToClientsTracking(T packet, BlockEntity be) {
        channel.sendToPlayers(PlayerLookup.tracking(be), packet);
    }

    public static <T> void sendToClientsTracking(T packet, Entity e) {
        channel.sendToPlayers(PlayerLookup.tracking(e), packet);
    }

    public static <T> void sendToClientsAround(T packet, ServerWorld world, Vec3d position, double radius) {
        channel.sendToPlayers(PlayerLookup.around(world, position, radius), packet);
    }

    public static <T> void sendToClientsAround(T packet, ServerWorld world, Vec3i position, double radius) {
        channel.sendToPlayers(PlayerLookup.around(world, position, radius), packet);
    }
}
