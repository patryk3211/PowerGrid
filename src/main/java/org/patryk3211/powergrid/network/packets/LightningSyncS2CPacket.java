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

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import org.patryk3211.powergrid.equipment.thunder.LightningRodMovementBehaviour;
import org.patryk3211.powergrid.network.ClientBoundPackets;

public class LightningSyncS2CPacket extends SimplePacketBase {
    private final int entityId;

    public LightningSyncS2CPacket(MovementContext context) {
        entityId = context.contraption.entity.getId();
    }

    public LightningSyncS2CPacket(PacketByteBuf buffer) {
        entityId = buffer.readInt();
    }

    @Override
    public void write(PacketByteBuf buffer) {
        buffer.writeInt(entityId);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            var world = ClientBoundPackets.world();
            if(world.getEntityById(entityId) instanceof AbstractContraptionEntity entity) {
                entity.getContraption().forEachActor(world, (behaviour, movementContext) -> {
                    if(!(behaviour instanceof LightningRodMovementBehaviour lightningBehaviour))
                        return;
                    // Lightning resets charge
                    movementContext.data.putFloat("Charge", 0);
                    if(movementContext.temporaryData == movementContext) {
                        // Controller
                        lightningBehaviour.fireClient(movementContext);
                    }
                });
            }
        });
        return true;
    }
}
