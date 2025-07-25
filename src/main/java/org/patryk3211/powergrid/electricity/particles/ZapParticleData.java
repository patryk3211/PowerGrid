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
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleData;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.AbstractDustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.dynamic.Codecs;
import org.joml.Vector3f;
import org.patryk3211.powergrid.collections.ModdedParticles;

public class ZapParticleData implements ParticleEffect, ICustomParticleData<ZapParticleData> {
    public static final Factory<ZapParticleData> FACTORY = new Factory<>() {
        @Override
        public ZapParticleData read(ParticleType<ZapParticleData> type, StringReader reader) throws CommandSyntaxException {
            return new ZapParticleData(AbstractDustParticleEffect.readColor(reader), true, 1);
        }

        @Override
        public ZapParticleData read(ParticleType<ZapParticleData> type, PacketByteBuf buf) {
            return new ZapParticleData(buf.readVector3f(), buf.readBoolean(), buf.readInt());
        }
    };
    private static final Codec<ZapParticleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codecs.VECTOR_3F.fieldOf("end").forGetter(ZapParticleData::getEnd),
            Codec.BOOL.fieldOf("anchor").forGetter(ZapParticleData::isAnchored),
            Codec.INT.fieldOf("life").forGetter(ZapParticleData::getLife)
    ).apply(instance, ZapParticleData::new));

    private final Vector3f end;
    private final boolean anchor;
    private int life;

    public ZapParticleData() {
        this(null, false, 1);
    }

    public ZapParticleData(Vector3f end, boolean anchor, int life) {
        this.end = end;
        this.anchor = anchor;
        this.life = life;
    }

    public ZapParticleData(float x, float y, float z, boolean anchor) {
        this(new Vector3f(x, y, z), anchor, 1);
    }

    public ZapParticleData(double x, double y, double z, boolean anchor) {
        this(new Vector3f((float) x, (float) y, (float) z), anchor, 1);
    }

    public ZapParticleData withLife(int life) {
        this.life = life;
        return this;
    }

    public Vector3f getEnd() {
        return end;
    }

    public boolean isAnchored() {
        return anchor;
    }

    public int getLife() {
        return life;
    }

    @Override
    public Factory<ZapParticleData> getDeserializer() {
        return FACTORY;
    }

    @Override
    public Codec<ZapParticleData> getCodec(ParticleType<ZapParticleData> type) {
        return CODEC;
    }

    @Override
    public ParticleFactory<ZapParticleData> getFactory() {
        return ZapParticle::new;
    }

    @Override
    public ParticleType<?> getType() {
        return ModdedParticles.ZAP;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVector3f(end);
        buf.writeBoolean(anchor);
        buf.writeInt(life);
    }

    @Override
    public String asString() {
        if(end == null) {
            return Registries.PARTICLE_TYPE.getId(getType()).toString();
        } else {
            return String.format("%s (%f, %f, %f)", Registries.PARTICLE_TYPE.getId(getType()), end.x, end.y, end.z);
        }
    }
}
