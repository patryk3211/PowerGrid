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

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.IMultiScreenHandlerFactory;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableBlockEntity;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.ISchematicHolder;
import org.patryk3211.powergrid.network.C2SPacket;

public class SaveSchematicC2SPacket implements C2SPacket {
    private final BlockPos pos;
    private CompoundTag nbt;
    private String name;
    private boolean load;

    public <T extends BlockEntity&ISchematicHolder> SaveSchematicC2SPacket(T be, boolean load) {
        pos = be.getBlockPos();
        nbt = null;
        this.load = load;
    }

    public <T extends BlockEntity&ISchematicHolder> SaveSchematicC2SPacket(T be, @Nullable String name, CircuitSchematic schematic) {
        pos = be.getBlockPos();
        nbt = schematic.serializeNbt(be.getLevel().registryAccess());
        this.name = name;
    }

    public SaveSchematicC2SPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        if(buf.readBoolean()) {
            nbt = buf.readNbt();
            if(buf.readBoolean()) {
                name = buf.readUtf();
            }
        } else {
            load = buf.readBoolean();
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(nbt != null);
        if(nbt != null) {
            buf.writeNbt(nbt);
            buf.writeBoolean(name != null);
            if(name != null) {
                buf.writeUtf(name);
            }
        } else {
            buf.writeBoolean(load);
        }
    }

    @Override
    public void handle(ServerPlayer player) {
        var be = player.serverLevel().getBlockEntity(pos);
        if(be instanceof CircuitDesignTableBlockEntity table) {
            if(nbt != null) {
                table.getSchematic().deserializeNbt(player.serverLevel().registryAccess(), nbt);
                table.setSchematicName(name);
                table.notifyUpdate();

                // Saved successfully.
                IMultiScreenHandlerFactory.openScreen(player, table, table::sendToMenu, 0);
            } else {
                if(load) {
                    // Load schematic from item
                    table.readFromItem();
                } else {
                    // Save schematic to item
                    table.writeToItem(player.isCreative());
                }
            }
        } else if(be instanceof ISchematicHolder holder) {
            if(player.isCreative() && nbt != null) {
                holder.setSchematic(CircuitSchematic.fromNbt(player.serverLevel().registryAccess(), nbt));
            }
        }
    }
}
