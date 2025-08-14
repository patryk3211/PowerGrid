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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.ArrayList;
import java.util.function.Supplier;

public class BlockWireCutC2SPacket implements SimplePacket {
    public final int entityId;
    public final int index1;
    public final int point1;
    public final int index2;
    public final int point2;

    public BlockWireCutC2SPacket(FriendlyByteBuf buf) {
        entityId = buf.readInt();
        index1 = buf.readInt();
        point1 = buf.readInt();
        index2 = buf.readInt();
        point2 = buf.readInt();
    }

    public BlockWireCutC2SPacket(BlockWireEntity entity, int index1, int point1, int index2, int point2) {
        this.entityId = entity.getId();
        if(index2 < index1) {
            this.index1 = index2;
            this.point1 = point2;
            this.index2 = index1;
            this.point2 = point1;
        } else {
            this.index1 = index1;
            this.point1 = point1;
            this.index2 = index2;
            this.point2 = point2;
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
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
            var entity = ctx.getPlayer().level().getEntity(entityId);
            if(!(entity instanceof BlockWireEntity wire)) {
                PowerGrid.LOGGER.error("Received block wire cut packet with invalid entity");
                return;
            }
            int wireCount = wire.getWireCount();
            int gridLength = 0;
            var secondSegments = new ArrayList<BlockWireEntity.Point>();
            Vec3 secondStart = null;
            for(int i = index2; i < wire.segments.size(); ++i) {
                var segment = wire.segments.get(i);
                if(i == index2) {
                    secondStart = segment.start.relative(segment.direction, point2 / 16f);
                    var len = segment.gridLength - point2;
                    if(len > 0) {
                        secondSegments.add(new BlockWireEntity.Point(segment.direction, gridLength));
                        gridLength += len;
                    }
                } else {
                    secondSegments.add(new BlockWireEntity.Point(segment.direction, segment.gridLength));
                    gridLength += segment.gridLength;
                }
            }
            int wire2Count = (int) Math.ceil(gridLength / 16f);
            wireCount -= wire2Count;
            gridLength = 0;
            while(wire.segments.size() > index1 + 1) {
                // Remove all segments above index1
                wire.segments.remove(wire.segments.size() - 1);
            }
            var last = wire.segments.remove(wire.segments.size() - 1);
            wire.segments.add(new BlockWireEntity.Point(last.direction, Math.min(last.gridLength, point1)));
            for(var segment : wire.segments) {
                gridLength += segment.gridLength;
            }
            int wire1Count = (int) Math.ceil(gridLength / 16f);
            wireCount -= wire1Count;

            wire.sendExtraData();
        });
    }
}
