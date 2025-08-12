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
package org.patryk3211.powergrid.collections.fabric;

import com.simibubi.create.foundation.particle.ICustomParticleData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModdedParticlesImpl {
    private static final List<ParticleEntry<?>> all = new ArrayList<>();

    public static <T extends ParticleOptions> void addEntry(ParticleType<T> type, Supplier<? extends ICustomParticleData<T>> typeFactory) {
        all.add(new ParticleEntry<>(type, typeFactory));
    }

    @Environment(EnvType.CLIENT)
    public static void registerFactories() {
        var manager = Minecraft.getInstance().particleEngine;
        for(var entry : all) {
            entry.registerFactory(manager);
        }
    }

    private record ParticleEntry<T extends ParticleOptions>(ParticleType<T> type, Supplier<? extends ICustomParticleData<T>> typeFactory) {
        @Environment(EnvType.CLIENT)
        public void registerFactory(ParticleEngine particleManager) {
            typeFactory.get().register(type, particleManager);
        }
    }
}
