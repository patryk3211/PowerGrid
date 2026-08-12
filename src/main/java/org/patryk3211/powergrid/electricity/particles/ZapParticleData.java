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
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.DustParticleOptionsBase;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.collections.ModdedParticles;

public class ZapParticleData implements ParticleOptions, ICustomParticleData<ZapParticleData> {
    public static final Deserializer<ZapParticleData> FACTORY = new Deserializer<>() {
        @Override
        public ZapParticleData fromCommand(ParticleType<ZapParticleData> type, StringReader reader) throws CommandSyntaxException {
            var vec = DustParticleOptionsBase.readVector3f(reader);
            return new ZapParticleData(vec.x, vec.y, vec.z, true, 1, -1, 1);
        }

        @Override
        public ZapParticleData fromNetwork(ParticleType<ZapParticleData> type, FriendlyByteBuf buf) {
            return new ZapParticleData(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readFloat());
        }
    };
    private static final Codec<ZapParticleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(data -> data.getEnd().x),
            Codec.DOUBLE.fieldOf("y").forGetter(data -> data.getEnd().y),
            Codec.DOUBLE.fieldOf("z").forGetter(data -> data.getEnd().z),
            Codec.BOOL.fieldOf("anchor").forGetter(ZapParticleData::isAnchored),
            Codec.INT.fieldOf("life").forGetter(ZapParticleData::getLife),
            Codec.INT.fieldOf("segments").forGetter(ZapParticleData::getSegmentCount),
            Codec.FLOAT.fieldOf("factor").forGetter(ZapParticleData::getFactor)
    ).apply(instance, ZapParticleData::new));

    private final Vec3 end;
    private final boolean anchor;
    private int life;
    private int segmentCount;
    private float factor;

    public ZapParticleData() {
        this(null, false, 1);
    }

    public ZapParticleData(Vec3 end, boolean anchor) {
        this(end, anchor, 1, -1);
    }

    public ZapParticleData(Vec3 end, boolean anchor, int life) {
        this(end, anchor, life, -1);
    }

    public ZapParticleData(Vec3 end, boolean anchor, int life, int segmentCount) {
        this.end = end;
        this.anchor = anchor;
        this.life = life;
        this.segmentCount = segmentCount;
    }

    public ZapParticleData(Vec3 end, boolean anchor, int life, int segmentCount, float factor) {
        this.end = end;
        this.anchor = anchor;
        this.life = life;
        this.segmentCount = segmentCount;
        this.factor = factor;
    }

    public ZapParticleData(double x, double y, double z, boolean anchor, int life, int segmentCount, float factor) {
        this(new Vec3(x, y, z), anchor, life, segmentCount, factor);
    }

    public ZapParticleData(double x, double y, double z, boolean anchor) {
        this(new Vec3(x, y, z), anchor, 1);
    }

    public ZapParticleData withLife(int life) {
        this.life = life;
        return this;
    }

    public ZapParticleData withSegments(int count) {
        this.segmentCount = count;
        return this;
    }

    public Vec3 getEnd() {
        return end;
    }

    public boolean isAnchored() {
        return anchor;
    }

    public int getLife() {
        return life;
    }

    public int getSegmentCount() {
        return segmentCount;
    }

    public float getFactor() {
        return factor;
    }

    @Override
    public Deserializer<ZapParticleData> getDeserializer() {
        return FACTORY;
    }

    @Override
    public Codec<ZapParticleData> getCodec(ParticleType<ZapParticleData> type) {
        return CODEC;
    }

    @Override
    public ParticleProvider<ZapParticleData> getFactory() {
        return ZapParticle::new;
    }

    @Override
    public ParticleType<?> getType() {
        return ModdedParticles.ZAP;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeDouble(end.x);
        buf.writeDouble(end.y);
        buf.writeDouble(end.z);
        buf.writeBoolean(anchor);
        buf.writeInt(life);
        buf.writeInt(segmentCount);
        buf.writeFloat(factor);
    }

    @Override
    public String writeToString() {
        if(end == null) {
            return BuiltInRegistries.PARTICLE_TYPE.getKey(getType()).toString();
        } else {
            return String.format("%s (%f, %f, %f)", BuiltInRegistries.PARTICLE_TYPE.getKey(getType()), end.x, end.y, end.z);
        }
    }
}
