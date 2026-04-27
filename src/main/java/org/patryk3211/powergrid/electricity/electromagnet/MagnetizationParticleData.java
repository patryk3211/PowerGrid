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

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.collections.ModdedParticles;

public class MagnetizationParticleData implements ICustomParticleDataWithSprite<MagnetizationParticleData>, ParticleOptions {
    public static final Codec<MagnetizationParticleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(MagnetizationParticleData::getControllerPos)
    ).apply(instance, MagnetizationParticleData::new));
    public static final Deserializer<MagnetizationParticleData> FACTORY = new Deserializer<>() {
        @Override
        public MagnetizationParticleData fromCommand(ParticleType<MagnetizationParticleData> type, StringReader reader) throws CommandSyntaxException {
            return new MagnetizationParticleData();
        }

        @Override
        public MagnetizationParticleData fromNetwork(ParticleType<MagnetizationParticleData> type, FriendlyByteBuf buf) {
            if(buf.readBoolean())
                return new MagnetizationParticleData(buf.readBlockPos());
            else return new MagnetizationParticleData();
        }
    };

    private final BlockPos controller;

    public MagnetizationParticleData() {
        this(null);
    }

    public MagnetizationParticleData(BlockPos controller) {
        this.controller = controller;
    }

    @Override
    public ParticleOptions.Deserializer<MagnetizationParticleData> getDeserializer() {
        return FACTORY;
    }

    @Override
    public Codec<MagnetizationParticleData> getCodec(ParticleType<MagnetizationParticleData> type) {
        return CODEC;
    }

    @Override
    public ParticleEngine.SpriteParticleRegistration<MagnetizationParticleData> getMetaFactory() {
        return MagnetizationParticle.Factory::new;
    }

    @Override
    public ParticleType<?> getType() {
        return ModdedParticles.MAGNETIZATION;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        if(controller != null) {
            buf.writeBoolean(true);
            buf.writeBlockPos(controller);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public String writeToString() {
        if(controller != null)
            return String.format("%s (%d, %d, %d)", BuiltInRegistries.PARTICLE_TYPE.getKey(getType()), controller.getX(), controller.getY(), controller.getZ());
        else
            return String.format("%s", BuiltInRegistries.PARTICLE_TYPE.getKey(getType()));
    }

    public BlockPos getControllerPos() {
        return controller;
    }
}
