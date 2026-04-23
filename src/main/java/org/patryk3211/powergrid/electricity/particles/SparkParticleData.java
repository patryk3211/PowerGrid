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
package org.patryk3211.powergrid.electricity.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.collections.ModdedParticles;

public class SparkParticleData implements ParticleOptions, ICustomParticleDataWithSprite<SparkParticleData> {
    public static final SparkParticleData INSTANCE = new SparkParticleData();
    public static final Codec<SparkParticleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("life").forGetter(SparkParticleData::getLife),
            Codec.BOOL.fieldOf("collision").forGetter(SparkParticleData::getCollision),
            Codec.BOOL.fieldOf("gravity").forGetter(SparkParticleData::getGravity)
    ).apply(instance, SparkParticleData::new));

    public static final StreamCodec<ByteBuf, SparkParticleData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    public static final MapCodec<SparkParticleData> MAP_CODEC = CODEC.fieldOf("spark");

    private final int life;
    private final boolean collision;
    private final boolean gravity;

    public SparkParticleData() {
        this(-1, true, true);
    }

    public SparkParticleData(int life, boolean collision, boolean gravity) {
        this.life = life;
        this.collision = collision;
        this.gravity = gravity;
    }

    public int getLife() {
        return life;
    }

    public boolean getCollision() {
        return collision;
    }

    public boolean getGravity() {
        return gravity;
    }

    public static void explodeParticles(Level world, double x, double y, double z, Direction dir, int count) {
        var r = world.random;
        var offset = dir.step().mul(0.1f);
        for(int i = 0; i < count; ++i) {
            var heading = dir.step();
            var pitch = (float) ((r.nextFloat() - 0.5f) * Math.PI) * 0.9f;
            var yaw = (float) ((r.nextFloat() - 0.5f) * Math.PI) * 0.9f;
            heading.rotateX(pitch).rotateY(yaw);
            heading.mul(r.nextFloat() * 1.0f);
            world.addParticle(SparkParticleData.INSTANCE, x + offset.x, y + offset.y, z + offset.z, heading.x, heading.y, heading.z);
        }
    }

    @Override
    public ParticleType<?> getType() {
        return ModdedParticles.CUBE_SPARK;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, SparkParticleData> getStreamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public MapCodec<SparkParticleData> getCodec(ParticleType<SparkParticleData> type) {
        return MAP_CODEC;
    }

    @Override
    public ParticleEngine.SpriteParticleRegistration<SparkParticleData> getMetaFactory() {
        return SparkParticle.Factory::new;
    }
}
