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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.base.IMultiScreenHandlerFactory;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableBlockEntity;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;

public class SaveSchematicC2SPacket extends SimplePacketBase {
    private final BlockPos pos;
    private NbtCompound nbt;
    private boolean load;

    public SaveSchematicC2SPacket(CircuitDesignTableBlockEntity be, boolean load) {
        pos = be.getPos();
        nbt = null;
        this.load = load;
    }

    public SaveSchematicC2SPacket(CircuitDesignTableBlockEntity be, CircuitSchematic schematic) {
        pos = be.getPos();
        nbt = schematic.serializeNbt();
    }

    public SaveSchematicC2SPacket(PacketByteBuf buf) {
        pos = buf.readBlockPos();
        if(buf.readBoolean()) {
            nbt = buf.readNbt();
        } else {
            load = buf.readBoolean();
        }
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(nbt != null);
        if(nbt != null) {
            buf.writeNbt(nbt);
        } else {
            buf.writeBoolean(load);
        }
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            var world = context.sender().getWorld();
            var be = world.getBlockEntity(pos);
            if(be instanceof CircuitDesignTableBlockEntity table) {
                if(nbt != null) {
                    table.getSchematic().deserializeNbt(nbt);
                    table.notifyUpdate();

                    // Saved successfully.
                    IMultiScreenHandlerFactory.openScreen(context.sender(), table, table::sendToMenu, 0);
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
        return true;
    }
}
