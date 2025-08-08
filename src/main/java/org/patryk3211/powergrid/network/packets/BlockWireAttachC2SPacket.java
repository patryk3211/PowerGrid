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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.wire.*;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class BlockWireAttachC2SPacket implements SimplePacket {
    public final int entityId;
    public final int index;
    public final int gridPoint;

    public BlockWireAttachC2SPacket(BlockWireEntity entity, int index, int gridPoint) {
        this.entityId = entity.getId();
        this.index = index;
        this.gridPoint = gridPoint;
    }

    public BlockWireAttachC2SPacket(PacketByteBuf buf) {
        entityId = buf.readInt();
        index = buf.readInt();
        gridPoint = buf.readInt();
    }

    @Override
    public void encode(PacketByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(index);
        buf.writeInt(gridPoint);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            var player = ctx.getPlayer();
            var entity = player.getWorld().getEntityById(entityId);
            if(!(entity instanceof BlockWireEntity wire)) {
                PowerGrid.LOGGER.error("Received block wire attach packet with invalid entity");
                return;
            }
            var stack = player.getStackInHand(Hand.MAIN_HAND);
            if(!(stack.getItem() instanceof WireItem)) {
                PowerGrid.LOGGER.error("Received wire attach packet for player whose not holding a wire");
                return;
            }
            if(index < 0 || index >= wire.segments.size()) {
                PowerGrid.LOGGER.error("Received wire segment index out of bounds");
                return;
            }
            var segment = wire.segments.get(index);
            // Align to grid.
            var gridLength = segment.gridLength;
            if(gridPoint < 0 || gridPoint > gridLength) {
                PowerGrid.LOGGER.error("Received wire segment length out of bounds");
                return;
            }

            var existingEndpoint = WireEndpointType.deserialize(stack.getNbt());

            IWireEndpoint endpoint;
            if(gridPoint <= 1 && index == 0) {
                // Extend wire at start.
                if(wire.getEndpoint1() == null) {
                    wire = wire.flip();
                    endpoint = new BlockWireEntityEndpoint(wire, true);
                } else {
                    // Possibly a junction.
                    endpoint = wire.getEndpoint1();
                }
            } else if(gridPoint >= segment.gridLength - 1 && index == wire.segments.size() - 1){
                // Extend wire at end.
                if(wire.getEndpoint2() == null) {
                    endpoint = new BlockWireEntityEndpoint(wire, true);
                } else {
                    // Possibly a junction.
                    endpoint = wire.getEndpoint2();
                }
            } else {
                // Junction.
                endpoint = new DeferredJunctionWireEndpoint(wire, index, gridPoint);
            }
            if(endpoint != null && existingEndpoint == null) {
                stack.setNbt(endpoint.serialize());
            } else if(endpoint != null) {
                var result = WireItem.connect(player.getWorld(), stack, player, existingEndpoint, endpoint);
                if(result.getResult().isAccepted()) {
                    stack.setNbt(null);
                }
            }
        });
    }
}
