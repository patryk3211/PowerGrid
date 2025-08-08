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
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class BlockWireCutC2SPacket implements SimplePacket {
    public final int entityId;
    public final int index1;
    public final int point1;
    public final int index2;
    public final int point2;

    public BlockWireCutC2SPacket(PacketByteBuf buf) {
        entityId = buf.readInt();
        index1 = buf.readInt();
        point1 = buf.readInt();
        index2 = buf.readInt();
        point2 = buf.readInt();
    }

    public BlockWireCutC2SPacket(BlockWireEntity entity, int index1, int point1, int index2, int point2) {
        this.entityId = entity.getId();
        this.index1 = index1;
        this.point1 = point1;
        this.index2 = index2;
        this.point2 = point2;
    }

    @Override
    public void encode(PacketByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(index1);
        buf.writeInt(point1);
        buf.writeInt(index2);
        buf.writeInt(point2);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            var entity = ctx.getPlayer().getWorld().getEntityById(entityId);
            if(!(entity instanceof BlockWireEntity wire)) {
                PowerGrid.LOGGER.error("Received block wire cut packet with invalid entity");
                return;
            }
        });
    }
}
