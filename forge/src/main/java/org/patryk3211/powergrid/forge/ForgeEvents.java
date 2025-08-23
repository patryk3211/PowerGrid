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

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

public class ForgeEvents {
    @SubscribeEvent
    public static void entityUnloadEvent(EntityLeaveLevelEvent event) {
        if(event.getLevel() instanceof ServerLevel world) {
            WireEntity.entityUnload(event.getEntity(), world);
        }
    }

    @SubscribeEvent
    public static void chunkTicketUpdate(ChunkTicketLevelUpdatedEvent event) {
        if(event.getChunkHolder() == null)
            return;
        var chunk = event.getChunkHolder().getTickingChunk();
        if(chunk == null)
            return;
        if(event.getNewTicketLevel() >= 33 && event.getOldTicketLevel() <= 32) {
            // Block entities no longer ticking.
            // Above level 33 the entities get completely unloaded so no need to pause them.
            for(var be : chunk.getBlockEntities().values()) {
                if(be instanceof SmartBlockEntity smart) {
                    var electric = smart.getBehaviour(ElectricBehaviour.TYPE);
                    if(electric == null)
                        continue;
                    electric.pause();
                }
            }
        } else if(event.getNewTicketLevel() <= 32) {
            // Block entities ticking again.
            for(var be : chunk.getBlockEntities().values()) {
                if(be instanceof SmartBlockEntity smart) {
                    var electric = smart.getBehaviour(ElectricBehaviour.TYPE);
                    if(electric == null)
                        continue;
                   electric.unpause();
                }
            }
        }
    }
}
