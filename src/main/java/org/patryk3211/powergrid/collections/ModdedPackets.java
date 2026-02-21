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

import com.simibubi.create.Create;
import dev.architectury.networking.NetworkChannel;
import dev.architectury.networking.NetworkManager;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.equipment.zapper.ElectroZapperS2CPacket;
import org.patryk3211.powergrid.utility.PlayerLookup;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public enum ModdedPackets implements BasePacketPayload.PacketTypeProvider {
    ELECTRO_ZAPPER_SHOOT(ElectroZapperS2CPacket.class, ElectroZapperS2CPacket.STREAM_CODEC, null),
    ;

    private final CatnipPacketRegistry.PacketType<?> type;
    private final PacketType<?> type_legacy;

    <T extends BasePacketPayload> ModdedPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PacketType<?> typeLegacy) {
        type_legacy = typeLegacy;
        String name = this.name().toLowerCase(Locale.ROOT);
        this.type = new CatnipPacketRegistry.PacketType<>(
                new CustomPacketPayload.Type<>(
                    Create.asResource(name)),
                    clazz, codec
        );
    }

    public static void register() {
        CatnipPacketRegistry packetRegistry = new CatnipPacketRegistry(Create.ID, 1);
        for (ModdedPackets packet : ModdedPackets.values()) {
            packetRegistry.registerPacket(packet.type);
        }
        packetRegistry.registerAllPackets();
    }

    // Old stuff that should probably be removed
    public static final ResourceLocation CHANNEL_NAME = PowerGrid.asResource("main");
    private static NetworkChannel channel;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) this.type.type();
    }

    public static NetworkChannel getChannel() {
        return channel;
    }

    public static class PacketType<T> {
        private final java.util.function.BiConsumer<T, FriendlyByteBuf> encoder;
        private final Function<FriendlyByteBuf, T> decoder;
        private final java.util.function.BiConsumer<T, Supplier<NetworkManager.PacketContext>> handler;
        private final Class<T> type;

        private PacketType(Class<T> type, java.util.function.BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkManager.PacketContext>> handler) {
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
//        ModPackets.PACKETS.send(packet);
    }

    public static <T> void sendToClient(T packet, ServerPlayer player) {
        ModPackets.PACKETS.sendTo(player, packet);
    }

    public static <T> void sendToClientsTracking(T packet, Entity e) {
        channel.sendToPlayers(PlayerLookup.tracking(e), packet);
    }

    public static <T> void sendToClientsAround(T packet, ServerLevel world, Vec3 position, double radius) {
        channel.sendToPlayers(PlayerLookup.around(world, position, radius), packet);
    }
}
