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
package org.patryk3211.powergrid.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.kinetics.generator.coil.CoilAggregate;
import org.patryk3211.powergrid.kinetics.generator.coil.CoilBlockEntity;
import org.patryk3211.powergrid.network.packets.AggregateCoilsS2CPacket;
import org.patryk3211.powergrid.network.packets.EntityDataS2CPacket;

public class ClientBoundPackets {
    public static void init() {
        entityDataPacket();
        aggregateCoilsPacket();
    }

    private static void aggregateCoilsPacket() {
        ClientPlayNetworking.registerGlobalReceiver(AggregateCoilsS2CPacket.TYPE, (packet, player, sender) -> {
            var world = player.getWorld();
            var aggregate = new CoilAggregate();
            for(BlockPos pos : packet.coilPositions) {
                if(!(world.getBlockEntity(pos) instanceof CoilBlockEntity coil)) {
                    PowerGrid.LOGGER.warn("Client block entity is not a coil entity and cannot be aggregated.");
                    continue;
                }
                aggregate.addCoil(coil);
            }
            aggregate.apply();
        });
    }

    private static void entityDataPacket() {
        ClientPlayNetworking.registerGlobalReceiver(ModdedPackets.ENTITY_DATA_PACKET, (client, handler, buf, response) -> {
            var packet = new EntityDataS2CPacket(buf);
            packet.buffer.retain();
            client.execute(() -> {
                if(client.world == null) {
                    PowerGrid.LOGGER.warn("Received entity data packet without a world");
                    return;
                }
                var entity = client.world.getEntityById(packet.entityId);
                if(entity instanceof EntityDataS2CPacket.IConsumer consumer) {
                    consumer.onEntityDataPacket(packet);
                }
                packet.buffer.release();
            });
        });
    }
}
