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

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;

public class ForgeEvents {
    @SubscribeEvent
    public static void entityUnloadEvent(EntityLeaveLevelEvent event) {
        if(event.getLevel() instanceof ServerLevel world) {
            BaseWireEntity.entityUnload(event.getEntity(), world);
        }
    }

    @SubscribeEvent
    public static void chunkTicketUpdate(ChunkTicketLevelUpdatedEvent event) {
        if(event.getChunkHolder() == null)
            return;
        ElectricBehaviour.handleTicketChange(event.getNewTicketLevel(), event.getChunkHolder(), event.getOldTicketLevel());
    }

    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load event) {
        var level = event.getLevel();
        if(level.isClientSide())
            return;
        var global = GlobalElectricNetworks.getWorldNetworks(level);
        if(global == null)
            return;
        global.chunkLoaded(event.getChunk().getPos());
    }
}
