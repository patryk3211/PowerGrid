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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.vat.particles.BubbleParticleData;
import org.patryk3211.powergrid.chemistry.vat.particles.GasParticleData;
import org.patryk3211.powergrid.electricity.electromagnet.MagnetizationParticleData;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModdedParticles {
    private static final List<ParticleEntry<?>> all = new ArrayList<>();

    public static final ParticleType<GasParticleData> GAS = register("gas", GasParticleData::new);
    public static final ParticleType<MagnetizationParticleData> MAGNETIZATION = register("magnetization", MagnetizationParticleData::new);
    public static final ParticleType<BubbleParticleData> VAT_BUBBLE = register("vat_bubble", BubbleParticleData::new);

    public static final ParticleType<SparkParticleData> CUBE_SPARK = register("spark", SparkParticleData::new);

    private static <T extends ParticleEffect> ParticleType<T> register(String name, Supplier<? extends ICustomParticleData<T>> typeFactory) {
        var type = Registry.register(Registries.PARTICLE_TYPE, PowerGrid.asResource(name), typeFactory.get().createType());
        all.add(new ParticleEntry<T>(type, typeFactory));
        return type;
    }

    @Environment(EnvType.CLIENT)
    public static void registerFactories() {
        var manager = MinecraftClient.getInstance().particleManager;
        for(var entry : all) {
            entry.registerFactory(manager);
        }
    }

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }

    private record ParticleEntry<T extends ParticleEffect>(ParticleType<T> type, Supplier<? extends ICustomParticleData<T>> typeFactory) {
        @Environment(EnvType.CLIENT)
        public void registerFactory(ParticleManager particleManager) {
            typeFactory.get().register(type, particleManager);
        }
    }
}
