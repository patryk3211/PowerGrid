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

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.network.ClientBoundPackets;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class UpdateComponentBiPacket implements SimplePacket {
    private final BlockPos pos;
    private final int componentId;
    private final ResourceLocation propertyId;
    private final CompoundTag propertyValue;

    public UpdateComponentBiPacket(CircuitBoardBlockEntity be, PlacedComponent component, ComponentProperty<?> property) {
        pos = be.getBlockPos();
        componentId = be.getSchematic().getId(component);
        assert componentId >= 0;
        propertyId = property.id();
        propertyValue = new CompoundTag();
        component.getEntry(property).write(propertyValue);
    }

    public UpdateComponentBiPacket(CircuitBoardBlockEntity be, PlacedComponent component, ResourceLocation propertyId) {
        pos = be.getBlockPos();
        componentId = be.getSchematic().getId(component);
        assert componentId >= 0;
        this.propertyId = propertyId;
        propertyValue = new CompoundTag();
        component.getEntry(propertyId).write(propertyValue);
    }

    public UpdateComponentBiPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        componentId = buf.readInt();
        propertyId = buf.readResourceLocation();
        propertyValue = buf.readNbt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(componentId);
        buf.writeResourceLocation(propertyId);
        buf.writeNbt(propertyValue);
    }

    public void handle(Level world) {
        var be = world.getBlockEntity(pos, ModdedBlockEntities.CIRCUIT_BOARD.get());
        be.ifPresent(circuit -> {
            var placed = circuit.getSchematic().components().get(componentId);
            var entry = placed.getEntry(propertyId);
            entry.read(propertyValue);
            placed.stateUpdated();
            if(!world.isClientSide) {
                // Server must broadcast this update to all clients
                placed.notifyClients(propertyId);
            }
        });
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        var world = ClientBoundPackets.world();
        handle(world);
    }

    public void handleServer(Player player) {
        var world = player.level();
        if(!player.mayInteract(world, pos))
            return;
        handle(world);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            switch (ctx.getEnv()) {
                case CLIENT -> handleClient();
                case SERVER -> handleServer(ctx.getPlayer());
            }
        });
    }
}
