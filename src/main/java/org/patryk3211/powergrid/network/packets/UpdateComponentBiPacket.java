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
package org.patryk3211.powergrid.network.packets;

import com.simibubi.create.foundation.mixin.fabric.BlockableEventLoopAccessor;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import me.pepperbell.simplenetworking.SimpleChannel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.network.ClientBoundPackets;

public class UpdateComponentBiPacket extends SimplePacketBase {
    private final BlockPos pos;
    private final int componentId;
    private final Identifier propertyId;
    private final NbtCompound propertyValue;

    public UpdateComponentBiPacket(CircuitBoardBlockEntity be, PlacedComponent component, ComponentProperty<?> property) {
        pos = be.getPos();
        componentId = be.getSchematic().getId(component);
        assert componentId >= 0;
        propertyId = property.id();
        propertyValue = new NbtCompound();
        component.getEntry(property).write(propertyValue);
    }

    public UpdateComponentBiPacket(CircuitBoardBlockEntity be, PlacedComponent component, Identifier propertyId) {
        pos = be.getPos();
        componentId = be.getSchematic().getId(component);
        assert componentId >= 0;
        this.propertyId = propertyId;
        propertyValue = new NbtCompound();
        component.getEntry(propertyId).write(propertyValue);
    }

    public UpdateComponentBiPacket(PacketByteBuf buf) {
        pos = buf.readBlockPos();
        componentId = buf.readInt();
        propertyId = buf.readIdentifier();
        propertyValue = buf.readNbt();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(componentId);
        buf.writeIdentifier(propertyId);
        buf.writeNbt(propertyValue);
    }


    public void handle(World world) {
        var be = world.getBlockEntity(pos, ModdedBlockEntities.CIRCUIT_BOARD.get());
        be.ifPresent(circuit -> {
            var placed = circuit.getSchematic().components().get(componentId);
            var entry = placed.getEntry(propertyId);
            entry.read(propertyValue);
            placed.stateUpdated();
            if(!world.isClient) {
                // Server must broadcast this update to all clients
                placed.notifyClients(propertyId);
            }
        });
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient client, ClientPlayNetworkHandler listener, PacketSender responseSender, SimpleChannel channel) {
        var world = ClientBoundPackets.world();
        if(client.isOnThread()) {
            handle(world);
        } else {
            ((BlockableEventLoopAccessor) client).callSubmitAsync(() -> handle(world));
        }
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler listener, PacketSender responseSender, SimpleChannel channel) {
        var world = player.getWorld();
        if(!player.canModifyAt(world, pos))
            return;
        if(server.isOnThread()) {
            handle(world);
        } else {
            ((BlockableEventLoopAccessor) server).callSubmitAsync(() -> handle(world));
        }
    }

    @Override
    public boolean handle(Context context) {
        return true;
    }
}
