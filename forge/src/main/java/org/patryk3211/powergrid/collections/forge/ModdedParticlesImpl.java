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
package org.patryk3211.powergrid.collections.forge;

import com.simibubi.create.foundation.particle.ICustomParticleData;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModdedParticlesImpl {
    private static final List<ParticleEntry<?>> all = new ArrayList<>();

    public static <T extends ParticleEffect> void addEntry(ParticleType<T> type, Supplier<? extends ICustomParticleData<T>> typeFactory) {
        all.add(new ParticleEntry<>(type, typeFactory));
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerFactories(RegisterParticleProvidersEvent event) {
        for(var entry : all) {
            entry.registerFactory(event);
        }
    }

    private record ParticleEntry<T extends ParticleEffect>(ParticleType<T> type, Supplier<? extends ICustomParticleData<T>> typeFactory) {
        @OnlyIn(Dist.CLIENT)
        public void registerFactory(RegisterParticleProvidersEvent event) {
            typeFactory.get().register(type, event);
        }
    }
}
