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
package org.patryk3211.powergrid.electricity.zapper;

import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonPacket;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.PowerGridClient;

public class ElectroZapperPacket extends PotatoCannonPacket {
    public ElectroZapperPacket(Vec3d location, Vec3d motion, ItemStack item, Hand hand, float pitch, boolean self) {
        super(location, motion, item, hand, pitch, self);
    }

    public ElectroZapperPacket(PacketByteBuf buffer) {
        super(buffer);
    }

    @Override
    protected void handleAdditional() {

    }

    @Override
    @Environment(EnvType.CLIENT)
    protected ShootableGadgetRenderHandler getHandler() {
        return PowerGridClient.ELECTRO_ZAPPER_RENDER_HANDLER;
    }
}
