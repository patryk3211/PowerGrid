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

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;

public class UpdateComponentS2CPacket extends SimplePacketBase {
    private final BlockPos pos;
    private final int componentId;
    private final Identifier propertyId;
    private final NbtCompound propertyValue;

    public UpdateComponentS2CPacket(CircuitBoardBlockEntity be, PlacedComponent component, ComponentProperty<?> property) {
        pos = be.getPos();
        componentId = be.getSchematic().getId(component);
        assert componentId >= 0;
        propertyId = property.id();
        propertyValue = new NbtCompound();
        component.getEntry(property).write(propertyValue);
    }

    public UpdateComponentS2CPacket(PacketByteBuf buf) {
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

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            var world = MinecraftClient.getInstance().world;
            var be = world.getBlockEntity(pos, ModdedBlockEntities.CIRCUIT_BOARD.get());
            be.ifPresent(circuit -> {
                var placed = circuit.getSchematic().components().get(componentId);
                var entry = placed.getEntry(propertyId);
                entry.read(propertyValue);
                placed.stateUpdated();
            });
        });
        return true;
    }
}
