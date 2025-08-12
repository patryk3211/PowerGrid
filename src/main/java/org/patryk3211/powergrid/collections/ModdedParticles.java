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
package org.patryk3211.powergrid.collections;

import com.simibubi.create.foundation.particle.ICustomParticleData;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.RegistryKeys;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.electromagnet.MagnetizationParticleData;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.particles.ZapParticleData;

import java.util.function.Supplier;

public class ModdedParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(PowerGrid.MOD_ID, RegistryKeys.PARTICLE_TYPE);

    public static final ParticleType<MagnetizationParticleData> MAGNETIZATION = register("magnetization", MagnetizationParticleData::new);

    public static final ParticleType<SparkParticleData> CUBE_SPARK = register("spark", SparkParticleData::new);
    public static final ParticleType<ZapParticleData> ZAP = register("zap", ZapParticleData::new);

    private static <T extends ParticleEffect> ParticleType<T> register(String name, Supplier<? extends ICustomParticleData<T>> typeFactory) {
        var type = typeFactory.get().createType();
        PARTICLE_TYPES.register(name, () -> type);
        addEntry(type, typeFactory);
        return type;
    }

    @ExpectPlatform
    public static <T extends ParticleEffect> void addEntry(ParticleType<T> type, Supplier<? extends ICustomParticleData<T>> typeFactory) {
        throw new AssertionError();
    }

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
