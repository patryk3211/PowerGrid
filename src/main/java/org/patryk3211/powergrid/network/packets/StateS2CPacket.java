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

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.network.SimplePacket;
import org.patryk3211.powergrid.utility.ClientSideAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class StateS2CPacket implements SimplePacket {
    private final List<BlockPos> positions = new ArrayList<>();
    private final ByteBuf data;
    private FriendlyByteBuf wrapper;
    private int lengthPosition;

    public StateS2CPacket() {
        this.data = PooledByteBufAllocator.DEFAULT.buffer();
    }

    public FriendlyByteBuf wrapper() {
        if(wrapper == null)
            wrapper = new FriendlyByteBuf(data);
        return wrapper;
    }

    public StateS2CPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        for(int i = 0; i < count; ++i) {
            positions.add(buf.readBlockPos());
        }
        int size = buf.readInt();
        data = PooledByteBufAllocator.DEFAULT.buffer(size, size);
        buf.readBytes(data, size);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(positions.size());
        for(var pos : positions) {
            buf.writeBlockPos(pos);
        }
        buf.writeInt(data.writerIndex());
        buf.writeBytes(data, data.writerIndex());
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            var level = ClientSideAccess.world();
            for(var pos : positions) {
                var entryLength = wrapper().readInt();
                var eb = BlockEntityBehaviour.get(level, pos, ElectricBehaviour.TYPE);
                if(eb == null) {
                    // Skip entry
                    wrapper().skipBytes(entryLength);
                } else {
                    var start = wrapper().readerIndex();
                    eb.readFromSync(wrapper());
                    var end = wrapper().readerIndex();
                    if(end - start > entryLength) {
                        PowerGrid.LOGGER.warn("Buffer read overrun (Entry of {} bytes, read {} bytes)", entryLength, end - start);
                        wrapper().readerIndex(start + entryLength);
                    } else if(end - start < entryLength) {
                        PowerGrid.LOGGER.warn("Buffer read underrun (Entry of {} bytes, read {} bytes)", entryLength, end - start);
                        wrapper().readerIndex(start + entryLength);
                    }
                }
            }
        });
    }

    public void begin(BlockPos pos) {
        positions.add(pos);
        lengthPosition = wrapper().writerIndex();
        wrapper().writeInt(0);
    }

    public void end() {
        var entryLength = wrapper().writerIndex() - lengthPosition - 4;
        wrapper.setInt(lengthPosition, entryLength);
    }
}
