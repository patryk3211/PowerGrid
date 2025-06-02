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
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.collections.ModdedParticles;

public class MagnetizationParticleData implements ICustomParticleDataWithSprite<MagnetizationParticleData>, ParticleEffect {
    public static final Codec<MagnetizationParticleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(MagnetizationParticleData::getControllerPos)
    ).apply(instance, MagnetizationParticleData::new));
    public static final Factory<MagnetizationParticleData> FACTORY = new Factory<>() {
        @Override
        public MagnetizationParticleData read(ParticleType<MagnetizationParticleData> type, StringReader reader) throws CommandSyntaxException {
            return new MagnetizationParticleData();
        }

        @Override
        public MagnetizationParticleData read(ParticleType<MagnetizationParticleData> type, PacketByteBuf buf) {
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
    public ParticleEffect.Factory<MagnetizationParticleData> getDeserializer() {
        return FACTORY;
    }

    @Override
    public Codec<MagnetizationParticleData> getCodec(ParticleType<MagnetizationParticleData> type) {
        return CODEC;
    }

    @Override
    public ParticleManager.SpriteAwareFactory<MagnetizationParticleData> getMetaFactory() {
        return MagnetizationParticle.Factory::new;
    }

    @Override
    public ParticleType<?> getType() {
        return ModdedParticles.MAGNETIZATION;
    }

    @Override
    public void write(PacketByteBuf buf) {
        if(controller != null) {
            buf.writeBoolean(true);
            buf.writeBlockPos(controller);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public String asString() {
        if(controller != null)
            return String.format("%s (%d, %d, %d)", Registries.PARTICLE_TYPE.getId(getType()), controller.getX(), controller.getY(), controller.getZ());
        else
            return String.format("%s", Registries.PARTICLE_TYPE.getId(getType()));
    }

    public BlockPos getControllerPos() {
        return controller;
    }
}
