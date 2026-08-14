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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Math;
import org.joml.Vector3f;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.particles.ZapParticleData;
import org.patryk3211.powergrid.network.SimplePacket;
import org.patryk3211.powergrid.utility.ClientSideAccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ZapProjectileS2CPacket implements SimplePacket {
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
        pos = hit.getLocation().toVector3f();
        dir = hit.getDirection();
    }

    public ZapProjectileS2CPacket(Entity target, Collection<Entity> affected) {
        type = Type.ENTITY_HIT;
        targetEntity = target.getId();
        affectedEntities = affected.stream().map(Entity::getId).collect(Collectors.toList());
    }

    public ZapProjectileS2CPacket(FriendlyByteBuf buf) {
        type = buf.readEnum(Type.class);
        switch(type) {
            case BLOCK_HIT -> {
                pos = buf.readVector3f();
                dir = buf.readEnum(Direction.class);
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
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        switch(type) {
            case BLOCK_HIT -> {
                buf.writeVector3f(pos);
                buf.writeEnum(dir);
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
    @Environment(EnvType.CLIENT)
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            var world = ClientSideAccess.world();
            switch(type) {
                case BLOCK_HIT -> SparkParticleData.explodeParticles(world, pos.x, pos.y, pos.z, dir, 20);
                case ENTITY_HIT -> {
                    var target = world.getEntity(targetEntity);
                    if(target == null)
                        return;
                    var origin = target.getBoundingBox().getCenter();
                    for(var id : affectedEntities) {
                        var entity = world.getEntity(id);
                        if(entity == null)
                            continue;
                        var end = entity.getBoundingBox().getCenter();
                        world.addAlwaysVisibleParticle(new ZapParticleData(end, true), true, origin.x, origin.y, origin.z, 0, 0, 0);
                        var r = world.random;
                        for(int i = 0; i < 10; ++i) {
                            float pos = r.nextFloat();
                            var x = Math.lerp(pos, origin.x, end.x);
                            var y = Math.lerp(pos, origin.y, end.y);
                            var z = Math.lerp(pos, origin.z, end.z);
                            world.addParticle(SparkParticleData.INSTANCE, x, y, z,
                                    r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f);
                        }
                    }
                }
            }
        });
    }
}
