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

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.base.IMultiScreenHandlerFactory;

public class ChangeScreenC2SPacket extends SimplePacketBase {
    private final BlockPos blockPos;
    private final int screenIndex;

    public <T extends SmartBlockEntity & IMultiScreenHandlerFactory> ChangeScreenC2SPacket(T be, int screenIndex) {
        this.blockPos = be.getPos();
        this.screenIndex = screenIndex;
    }

    public ChangeScreenC2SPacket(PacketByteBuf buf) {
        blockPos = buf.readBlockPos();
        screenIndex = buf.readInt();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeInt(screenIndex);
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            var player = context.sender();
            if(player == null)
                return;
            var genericBe = player.getWorld().getBlockEntity(blockPos);
            if(!(genericBe instanceof SmartBlockEntity) || !(genericBe instanceof IMultiScreenHandlerFactory))
                return;
            var be = (SmartBlockEntity & IMultiScreenHandlerFactory) genericBe;
            IMultiScreenHandlerFactory.openScreen(player, be, be::sendToMenu, screenIndex);
        });
        return true;
    }
}
