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
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.kinetics.generator.coil.CoilBlockEntity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class AggregateCoilsS2CPacket implements FabricPacket {
    public static final PacketType<AggregateCoilsS2CPacket> TYPE = PacketType.create(ModdedPackets.AGGREGATE_COILS_PACKET, AggregateCoilsS2CPacket::new);

    public final List<BlockPos> coilPositions = new LinkedList<>();

    public AggregateCoilsS2CPacket(PacketByteBuf buf) {
        int count = buf.readInt();
        for(int i = 0; i < count; ++i) {
            coilPositions.add(buf.readBlockPos());
        }
    }

    public AggregateCoilsS2CPacket(Collection<CoilBlockEntity> coils) {
        coils.forEach(coil -> coilPositions.add(coil.getPos()));
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(coilPositions.size());
        coilPositions.forEach(buf::writeBlockPos);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
