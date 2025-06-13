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
package org.patryk3211.powergrid.chemistry.vat.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.collections.ModdedParticles;

public class BubbleParticleData implements ICustomParticleDataWithSprite<BubbleParticleData>, ParticleEffect {
    public static final Codec<BubbleParticleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(BubbleParticleData::getVatPos)
    ).apply(instance, BubbleParticleData::new));
    public static final Factory<BubbleParticleData> FACTORY = new Factory<>() {
        @Override
        public BubbleParticleData read(ParticleType<BubbleParticleData> type, StringReader reader) throws CommandSyntaxException {
            var message = Text.of("Cannot spawn this particle with a command");
            throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
        }

        @Override
        public BubbleParticleData read(ParticleType<BubbleParticleData> type, PacketByteBuf buf) {
            return new BubbleParticleData(buf.readBlockPos());
        }
    };

    private final BlockPos vatPos;

    public BubbleParticleData() {
        this(null);
    }

    public BubbleParticleData(BlockPos vat) {
        vatPos = vat;
    }

    public BlockPos getVatPos() {
        return vatPos;
    }

    @Override
    public Factory<BubbleParticleData> getDeserializer() {
        return FACTORY;
    }

    @Override
    public Codec<BubbleParticleData> getCodec(ParticleType<BubbleParticleData> type) {
        return CODEC;
    }

    @Override
    public ParticleManager.SpriteAwareFactory<BubbleParticleData> getMetaFactory() {
        return BubbleParticle.Factory::new;
    }

    @Override
    public ParticleType<?> getType() {
        return ModdedParticles.VAT_BUBBLE;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(vatPos);
    }

    @Override
    public String asString() {
        return Registries.PARTICLE_TYPE.getId(getType()).toString();
    }
}
