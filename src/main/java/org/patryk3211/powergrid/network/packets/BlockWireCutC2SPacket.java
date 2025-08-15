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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.ArrayList;
import java.util.List;
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
            this.index2 = index2;
            if(index1 == index2 && point2 < point1) {
                this.point1 = point2;
                this.point2 = point1;
            } else {
                this.point1 = point1;
                this.point2 = point2;
            }
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

    private static BlockWireEntity spawnWire2(BlockWireEntity wire1, Vec3 start, int wireCount, List<BlockWireEntity.Point> segments) {
        if(start == null)
            return null;
        var entity = BlockWireEntity.create(wire1.level(), start, new ItemStack(wire1.getWireItem(), wireCount), segments);
        ((ServerLevel) wire1.level()).addFreshEntityWithPassengers(entity);
        entity.setEndpoint2(wire1.getEndpoint2());
        wire1.setEndpoint2(null);
        return entity;
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
                        secondSegments.add(new BlockWireEntity.Point(segment.direction, len));
                        gridLength += len;
                    }
                } else {
                    secondSegments.add(new BlockWireEntity.Point(segment.direction, segment.gridLength));
                    gridLength += segment.gridLength;
                }
            }
            int wire2Count = (int) Math.ceil(gridLength / 16f);
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
            if(wire1Count >= wire2Count) {
                // Wire1 is the largest
                wire.setItem(wire.getWireItem(), Math.min(wireCount, wire1Count));
                wireCount -= wire1Count;
                if(wireCount <= 0) {
                    // Wire2 is discarded - not enough items
                    wire.setEndpoint2(null);
                } else {
                    // Spawn wire2
                    var wire2 = spawnWire2(wire, secondStart, Math.min(wire2Count, wireCount), secondSegments);
                    wireCount -= wire2Count;
                }
            } else {
                // Wire2 is the largest
                var wire2 = spawnWire2(wire, secondStart, Math.min(wireCount, wire2Count), secondSegments);
                wireCount -= wire2Count;
                if(wireCount <= 0) {
                    // Wire1 is discarded - not enough items
                    wire.discard();
                } else {
                    // Keep wire1
                    wire.setItem(wire.getWireItem(), Math.min(wire1Count, wireCount));
                    wireCount -= wire1Count;
                }
            }
            if(wireCount > 0) {
                // Drop excess wires
                var cutter = ctx.getPlayer();
                for(; wireCount > 0; wireCount -= 64) {
                    cutter.getInventory().placeItemBackInInventory(new ItemStack(wire.getWireItem(), Math.min(wireCount, 64)));
                }
            }

            if(!wire.isRemoved()) {
                wire.sendExtraData();
            }
        });
    }
}
