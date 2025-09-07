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

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.equipment.multimeter.MultimeterItem;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class MultimeterDataC2SPacket implements SimplePacket {
    private final Vector3f point;
    private final int wire;

    public MultimeterDataC2SPacket(Vec3 point, WireEntity wire) {
        this.point = point.toVector3f();
        this.wire = wire.getId();
    }

    public MultimeterDataC2SPacket(FriendlyByteBuf buf) {
        point = buf.readVector3f();
        wire = buf.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVector3f(point);
        buf.writeInt(wire);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            var player = ctx.getPlayer();
            var stack = player.getMainHandItem();
            if(!(stack.getItem() instanceof MultimeterItem multimeter))
                return;
            var level = player.level();
            var entity = level.getEntity(wire);
            if(entity == null)
                return;
            if(multimeter.getMode(stack) != 1)
                multimeter.setMode(stack, 1);
            var data = multimeter.getModeData(stack);
            data.putFloat("X", point.x);
            data.putFloat("Y", point.y);
            data.putFloat("Z", point.z);
            data.putUUID("UUID", entity.getUUID());
        });
    }
}
