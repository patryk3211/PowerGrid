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

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;

public class ZapProjectileS2CPacket extends SimplePacketBase {
    private enum Type {
        BLOCK_HIT
    }

    private final Type type;
    private Vector3f pos;
    private Direction dir;

    public ZapProjectileS2CPacket(BlockHitResult hit) {
        type = Type.BLOCK_HIT;
        pos = hit.getPos().toVector3f();
        dir = hit.getSide();
    }

    public ZapProjectileS2CPacket(PacketByteBuf buf) {
        type = buf.readEnumConstant(Type.class);
        switch(type) {
            case BLOCK_HIT -> {
                pos = buf.readVector3f();
                dir = buf.readEnumConstant(Direction.class);
            }
        }
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(type);
        switch(type) {
            case BLOCK_HIT -> {
                buf.writeVector3f(pos);
                buf.writeEnumConstant(dir);
            }
        }
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            var world = MinecraftClient.getInstance().world;
            SparkParticleData.explodeParticles(world, pos.x, pos.y, pos.z, dir, 20);
        });
        return true;
    }
}
