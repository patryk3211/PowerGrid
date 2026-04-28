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
package org.patryk3211.powergrid.electricity.electromagnet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.patryk3211.powergrid.collections.ModdedParticles;

public class MagnetizationParticleData implements ICustomParticleDataWithSprite<MagnetizationParticleData>, ParticleOptions {
    public static final Codec<MagnetizationParticleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(MagnetizationParticleData::getControllerPos)
    ).apply(instance, MagnetizationParticleData::new));

    public static final StreamCodec<ByteBuf, MagnetizationParticleData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    public static final MapCodec<MagnetizationParticleData> MAP_CODEC = CODEC.fieldOf("magnetic");

    private final BlockPos controller;

    public MagnetizationParticleData() {
        this(null);
    }

    public MagnetizationParticleData(BlockPos controller) {
        this.controller = controller;
    }

    @Override
    public MapCodec<MagnetizationParticleData> getCodec(ParticleType<MagnetizationParticleData> type) {
        return MAP_CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, MagnetizationParticleData> getStreamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public ParticleEngine.SpriteParticleRegistration<MagnetizationParticleData> getMetaFactory() {
        return MagnetizationParticle.Factory::new;
    }

    @Override
    public ParticleType<?> getType() {
        return ModdedParticles.MAGNETIZATION;
    }

    public BlockPos getControllerPos() {
        return controller;
    }
}
