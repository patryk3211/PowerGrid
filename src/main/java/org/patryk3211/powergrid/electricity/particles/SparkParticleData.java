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

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedParticles;

public class SparkParticleData implements ParticleEffect, ICustomParticleDataWithSprite<SparkParticleData> {
    public static final Factory<SparkParticleData> FACTORY = new Factory<>() {
        @Override
        public SparkParticleData read(ParticleType<SparkParticleData> type, StringReader reader) throws CommandSyntaxException {
            return new SparkParticleData();
        }

        @Override
        public SparkParticleData read(ParticleType<SparkParticleData> type, PacketByteBuf buf) {
            return new SparkParticleData();
        }
    };
    public static final SparkParticleData INSTANCE = new SparkParticleData();
    public static final Codec<SparkParticleData> CODEC = Codec.unit(INSTANCE); //RecordCodecBuilder.create(instance -> instance.point(new CubeSparkParticleData()));

    public SparkParticleData() {

    }

    public static void explodeParticles(World world, float x, float y, float z, Direction dir, int count) {
        var r = world.random;
        var offset = dir.getUnitVector().mul(0.1f);
        for(int i = 0; i < count; ++i) {
            var heading = dir.getUnitVector();
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
    public void write(PacketByteBuf buf) {

    }

    @Override
    public String asString() {
        return Registries.PARTICLE_TYPE.getId(getType()).toString();
    }

    @Override
    public Factory<SparkParticleData> getDeserializer() {
        return FACTORY;
    }

    @Override
    public Codec<SparkParticleData> getCodec(ParticleType<SparkParticleData> type) {
        return CODEC;
    }

    @Override
    public ParticleManager.SpriteAwareFactory<SparkParticleData> getMetaFactory() {
        return SparkParticle.Factory::new;
    }
}
