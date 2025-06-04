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

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntityEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireItem;

public class BlockWireBeginC2SPacket implements FabricPacket {
    public static final PacketType<BlockWireBeginC2SPacket> TYPE = PacketType.create(ModdedPackets.BLOCK_WIRE_BEGIN, BlockWireBeginC2SPacket::new);

    public final int entityId;
    public final int index;
    public final int gridPoint;

    public BlockWireBeginC2SPacket(BlockWireEntity entity, int index, int gridPoint) {
        this.entityId = entity.getId();
        this.index = index;
        this.gridPoint = gridPoint;
    }

    public BlockWireBeginC2SPacket(PacketByteBuf buf) {
        entityId = buf.readInt();
        index = buf.readInt();
        gridPoint = buf.readInt();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(index);
        buf.writeInt(gridPoint);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static void handler(BlockWireBeginC2SPacket packet, ServerPlayerEntity player, PacketSender response) {
        var entity = player.getWorld().getEntityById(packet.entityId);
        if(!(entity instanceof BlockWireEntity wire)) {
            PowerGrid.LOGGER.error("Received block wire begin packet with invalid entity");
            return;
        }
        var stack = player.getStackInHand(Hand.MAIN_HAND);
        if(!(stack.getItem() instanceof WireItem)) {
            PowerGrid.LOGGER.error("Received wire begin packet for player whose not holding a wire");
            return;
        }
        if(packet.index < 0 || packet.index >= wire.segments.size()) {
            PowerGrid.LOGGER.error("Received wire segment index out of bounds");
            return;
        }
        var segment = wire.segments.get(packet.index);
        // Align to grid.
        var gridPoint = packet.gridPoint;
        var gridLength = segment.gridLength;
        if(gridPoint < 0 || gridPoint > gridLength) {
            PowerGrid.LOGGER.error("Received wire segment length out of bounds");
            return;
        }

        IWireEndpoint endpoint;
        if(gridPoint == 0 && packet.index == 0) {
            // Extend wire at start.
            wire = wire.flip();
            endpoint = new BlockWireEntityEndpoint(wire, true);
        } else if(gridPoint == segment.gridLength && packet.index == wire.segments.size() - 1){
            // Extend wire at end.
            endpoint = new BlockWireEntityEndpoint(wire, true);
        } else {
            // Junction.
            endpoint = null;
        }
        if(endpoint != null)
            stack.setNbt(endpoint.serialize());
    }
}
