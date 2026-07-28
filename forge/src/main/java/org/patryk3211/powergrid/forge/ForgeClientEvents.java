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
package org.patryk3211.powergrid.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.patryk3211.powergrid.electricity.ClientElectricNetwork;
import org.patryk3211.powergrid.network.packets.EntityDataS2CPacket;

public class ForgeClientEvents {
    @SubscribeEvent
    public static void clientWorldUnload(LevelEvent.Unload unload) {
        if(unload.getLevel().isClientSide() && unload.getLevel() instanceof ClientLevel world) {
            ClientElectricNetwork.unloadWorld(Minecraft.getInstance(), world);
            EntityDataS2CPacket.clearDeferredData();
        }
    }

    @SubscribeEvent
    public static void entityJoin(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide) {
            EntityDataS2CPacket.clientEntityAdded(event.getEntity());
        }
    }
}
