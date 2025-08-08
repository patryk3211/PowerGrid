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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.IMultiScreenHandlerFactory;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableBlockEntity;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class SaveSchematicC2SPacket implements SimplePacket {
    private final BlockPos pos;
    private NbtCompound nbt;
    @Nullable
    private String name;
    private boolean load;

    public SaveSchematicC2SPacket(CircuitDesignTableBlockEntity be, boolean load) {
        pos = be.getPos();
        nbt = null;
        this.load = load;
    }

    public SaveSchematicC2SPacket(CircuitDesignTableBlockEntity be, @Nullable String name, CircuitSchematic schematic) {
        pos = be.getPos();
        nbt = schematic.serializeNbt();
        this.name = name;
    }

    public SaveSchematicC2SPacket(PacketByteBuf buf) {
        pos = buf.readBlockPos();
        if(buf.readBoolean()) {
            nbt = buf.readNbt();
            if(buf.readBoolean()) {
                name = buf.readString();
            }
        } else {
            load = buf.readBoolean();
        }
    }

    @Override
    public void encode(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(nbt != null);
        if(nbt != null) {
            buf.writeNbt(nbt);
            buf.writeBoolean(name != null);
            if(name != null) {
                buf.writeString(name);
            }
        } else {
            buf.writeBoolean(load);
        }
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            var world = ctx.getPlayer().getWorld();
            var be = world.getBlockEntity(pos);
            if(be instanceof CircuitDesignTableBlockEntity table) {
                if(nbt != null) {
                    table.getSchematic().deserializeNbt(nbt);
                    table.setSchematicName(name);
                    table.notifyUpdate();

                    // Saved successfully.
                    IMultiScreenHandlerFactory.openScreen((ServerPlayerEntity) ctx.getPlayer(), table, table::sendToMenu, 0);
                } else {
                    if(load) {
                        // Load schematic from item
                        table.readFromItem();
                    } else {
                        // Save schematic to item
                        table.writeToItem();
                    }
                }
            }
        });
    }
}
