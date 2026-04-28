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
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.equipment.zapper.ElectroZapperS2CPacket;
import org.patryk3211.powergrid.network.C2SPacket;
import org.patryk3211.powergrid.network.S2CPacket;
import org.patryk3211.powergrid.utility.PlayerLookup;

import java.util.Locale;

public enum ModdedPackets implements BasePacketPayload.PacketTypeProvider {
    ELECTRO_ZAPPER_SHOOT(ElectroZapperS2CPacket.class, ElectroZapperS2CPacket.STREAM_CODEC),
    ;

    private final CatnipPacketRegistry.PacketType<?> type;

    <T extends BasePacketPayload> ModdedPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
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

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) this.type.type();
    }

    public static void sendToServer(C2SPacket packet) {
        ModPackets.PACKETS.send(packet);
    }

    public static void sendToClient(S2CPacket packet, ServerPlayer player) {
        ModPackets.PACKETS.sendTo(player, packet);
    }

    public static void sendToClient(CustomPacketPayload packet, ServerPlayer player) {
        player.connection.send(new ClientboundCustomPayloadPacket(packet));
    }

    public static void sendToClientsTracking(S2CPacket packet, Entity e) {
        for (ServerPlayer player : PlayerLookup.tracking(e)) {
            ModPackets.PACKETS.sendTo(player, packet);
        }
    }

    public static void sendToClientsTracking(CustomPacketPayload packet, Entity e) {
        var wrapped = new ClientboundCustomPayloadPacket(packet);
        for (ServerPlayer player : PlayerLookup.tracking(e)) {
            player.connection.send(wrapped);
        }
    }

    public static void sendToClientsAround(S2CPacket packet, ServerLevel world, Vec3 position, double radius) {
        for (ServerPlayer player : PlayerLookup.around(world, position, radius)) {
            ModPackets.PACKETS.sendTo(player, packet);
        }
    }

    public static void sendToClientsAround(CustomPacketPayload packet, ServerLevel world, Vec3 position, double radius) {
        var wrapped = new ClientboundCustomPayloadPacket(packet);
        for (ServerPlayer player : PlayerLookup.around(world, position, radius)) {
            player.connection.send(wrapped);
        }
    }
}
