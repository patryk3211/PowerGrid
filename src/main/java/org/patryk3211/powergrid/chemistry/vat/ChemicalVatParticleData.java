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
package org.patryk3211.powergrid.chemistry.vat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.AbstractDustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.dynamic.Codecs;
import org.joml.Vector3f;
import org.patryk3211.powergrid.collections.ModdedParticles;

public class ChemicalVatParticleData implements ICustomParticleDataWithSprite<ChemicalVatParticleData>, ParticleEffect {
    public static final Codec<ChemicalVatParticleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codecs.VECTOR_3F.fieldOf("color").forGetter(ChemicalVatParticleData::getColor)
    ).apply(instance, ChemicalVatParticleData::new));
    public static final Factory<ChemicalVatParticleData> FACTORY = new Factory<>() {
        @Override
        public ChemicalVatParticleData read(ParticleType<ChemicalVatParticleData> type, StringReader reader) throws CommandSyntaxException {
            Vector3f color = AbstractDustParticleEffect.readColor(reader);
            return new ChemicalVatParticleData(color);
        }

        @Override
        public ChemicalVatParticleData read(ParticleType<ChemicalVatParticleData> type, PacketByteBuf buf) {
            var color = buf.readVector3f();
            return new ChemicalVatParticleData(color);
        }
    };

    private final Vector3f color;

    public ChemicalVatParticleData() {
        this.color = new Vector3f();
    }

    public ChemicalVatParticleData(Vector3f color) {
        this.color = color;
    }

    public ChemicalVatParticleData(float r, float g, float b) {
        this.color = new Vector3f(r, g, b);
    }

    @Override
    public ParticleType<?> getType() {
        return ModdedParticles.GAS;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVector3f(color);
    }

    @Override
    public String asString() {
        return String.format("%s %.2f %.2f %.2f", Registries.PARTICLE_TYPE.getId(getType()), color.x(), color.y(), color.z());
    }

    public Vector3f getColor() {
        return color;
    }

    @Override
    public ParticleEffect.Factory<ChemicalVatParticleData> getDeserializer() {
        return FACTORY;
    }

    @Override
    public Codec<ChemicalVatParticleData> getCodec(ParticleType<ChemicalVatParticleData> type) {
        return CODEC;
    }

    @Override
    public ParticleManager.SpriteAwareFactory<ChemicalVatParticleData> getMetaFactory() {
        return ChemicalVatParticle.Factory::new;
    }
}
