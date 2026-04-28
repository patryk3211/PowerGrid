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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.equipment.multimeter.MultimeterItem;
import org.patryk3211.powergrid.network.C2SPacket;

public class MultimeterDataC2SPacket implements C2SPacket {
    private final Vector3f point;
    private final int wire;

    public MultimeterDataC2SPacket(Vec3 point, BaseWireEntity wire) {
        this.point = point.toVector3f();
        this.wire = wire.getId();
    }

    public MultimeterDataC2SPacket(FriendlyByteBuf buf) {
        point = buf.readVector3f();
        wire = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVector3f(point);
        buf.writeInt(wire);
    }

    @Override
    public void handle(ServerPlayer player) {
        var stack = player.getMainHandItem();
        if(!(stack.getItem() instanceof MultimeterItem multimeter))
            return;
        var level = player.serverLevel();
        var entity = level.getEntity(wire);
        if(entity == null)
            return;
        if(multimeter.getMode(stack) != 1)
            multimeter.setMode(stack, 1);
        var data = MultimeterItem.getModeData(stack);
        data.putFloat("X", point.x);
        data.putFloat("Y", point.y);
        data.putFloat("Z", point.z);
        data.putUUID("UUID", entity.getUUID());
        MultimeterItem.saveModeData(stack, data);
    }
}
