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
package org.patryk3211.powergrid.equipment.zapper;

import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonPacket;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.PowerGridClient;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class ElectroZapperS2CPacket extends PotatoCannonPacket implements SimplePacket {
    public ElectroZapperS2CPacket(Vec3 location, Vec3 motion, ItemStack item, InteractionHand hand, float pitch, boolean self) {
        super(location, motion, item, hand, pitch, self);
    }

    public ElectroZapperS2CPacket(FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    protected void handleAdditional() {

    }

    @Override
    @Environment(EnvType.CLIENT)
    protected ElectroZapperRenderHandler getHandler() {
        return PowerGridClient.ELECTRO_ZAPPER_RENDER_HANDLER;
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            Entity renderViewEntity = Minecraft.getInstance()
                    .getCameraEntity();
            if (renderViewEntity == null)
                return;
            if (renderViewEntity.position()
                    .distanceTo(location) > 100)
                return;

            var handler = getHandler();
            handleAdditional();
            if (self)
                handler.shoot(hand, location);
            else
                handler.playSound(hand, location);
        });
    }
}
