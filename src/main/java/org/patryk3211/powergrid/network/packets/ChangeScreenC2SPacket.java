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
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.patryk3211.powergrid.base.IMultiScreenHandlerFactory;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class ChangeScreenC2SPacket implements SimplePacket {
    private final BlockPos blockPos;
    private final int screenIndex;

    public <T extends SmartBlockEntity & IMultiScreenHandlerFactory> ChangeScreenC2SPacket(T be, int screenIndex) {
        this.blockPos = be.getBlockPos();
        this.screenIndex = screenIndex;
    }

    public ChangeScreenC2SPacket(FriendlyByteBuf buf) {
        blockPos = buf.readBlockPos();
        screenIndex = buf.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeInt(screenIndex);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            var player = ctx.getPlayer();
            if(player == null)
                return;
            var genericBe = player.level().getBlockEntity(blockPos);
            if(!(genericBe instanceof SmartBlockEntity) || !(genericBe instanceof IMultiScreenHandlerFactory))
                return;
            var be = (SmartBlockEntity & IMultiScreenHandlerFactory) genericBe;
            IMultiScreenHandlerFactory.openScreen((ServerPlayer) player, be, be::sendToMenu, screenIndex);
        });
    }
}
