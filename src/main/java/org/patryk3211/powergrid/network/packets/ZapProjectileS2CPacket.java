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
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.particles.ZapParticleData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ZapProjectileS2CPacket extends SimplePacketBase {
    private enum Type {
        BLOCK_HIT,
        ENTITY_HIT
    }

    private final Type type;
    private Vector3f pos;
    private Direction dir;

    private int targetEntity;
    private List<Integer> affectedEntities;

    public ZapProjectileS2CPacket(BlockHitResult hit) {
        type = Type.BLOCK_HIT;
        pos = hit.getPos().toVector3f();
        dir = hit.getSide();
    }

    public ZapProjectileS2CPacket(Entity target, Collection<Entity> affected) {
        type = Type.ENTITY_HIT;
        targetEntity = target.getId();
        affectedEntities = affected.stream().map(Entity::getId).collect(Collectors.toList());
    }

    public ZapProjectileS2CPacket(PacketByteBuf buf) {
        type = buf.readEnumConstant(Type.class);
        switch(type) {
            case BLOCK_HIT -> {
                pos = buf.readVector3f();
                dir = buf.readEnumConstant(Direction.class);
            }
            case ENTITY_HIT -> {
                targetEntity = buf.readInt();

                int len = buf.readInt();
                affectedEntities = new ArrayList<>();
                for(int i = 0; i < len; ++i) {
                    affectedEntities.add(buf.readInt());
                }
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
            case ENTITY_HIT -> {
                buf.writeInt(targetEntity);
                buf.writeInt(affectedEntities.size());
                for(var i : affectedEntities) {
                    buf.writeInt(i);
                }
            }
        }
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            var world = MinecraftClient.getInstance().world;
            switch(type) {
                case BLOCK_HIT -> SparkParticleData.explodeParticles(world, pos.x, pos.y, pos.z, dir, 20);
                case ENTITY_HIT -> {
                    var target = world.getEntityById(targetEntity);
                    if(target == null)
                        return;
                    var origin = target.getBoundingBox().getCenter();
                    for(var id : affectedEntities) {
                        var entity = world.getEntityById(id);
                        if(entity == null)
                            continue;
                        var end = entity.getBoundingBox().getCenter();
                        world.addImportantParticle(new ZapParticleData((float) end.x, (float) end.y, (float) end.z), true, origin.x, origin.y, origin.z, 0, 0, 0);
                        var r = world.random;
                        for(int i = 0; i < 10; ++i) {
                            float pos = r.nextFloat();
                            var x = MathHelper.lerp(pos, origin.x, end.x);
                            var y = MathHelper.lerp(pos, origin.y, end.y);
                            var z = MathHelper.lerp(pos, origin.z, end.z);
                            world.addParticle(SparkParticleData.INSTANCE, x, y, z,
                                    r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f);
                        }
                    }
                }
            }
        });
        return true;
    }
}
