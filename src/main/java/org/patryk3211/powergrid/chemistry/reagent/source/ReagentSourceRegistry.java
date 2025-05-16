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
package org.patryk3211.powergrid.chemistry.reagent.source;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.reagent.ReagentRegistry;

public class ReagentSourceRegistry {
    public static final RegistryKey<Registry<ReagentSource>> REGISTRY_KEY = RegistryKey.ofRegistry(new Identifier(PowerGrid.MOD_ID, "reagent_sources"));

    public static final Codec<ReagentSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Registries.ITEM.getCodec().fieldOf("item").forGetter(ReagentSource::item),
            ReagentRegistry.REGISTRY.getCodec().fieldOf("reagent").forGetter(ReagentSource::reagent),
            Codec.INT.fieldOf("amount").forGetter(ReagentSource::amount)
    ).apply(instance, ReagentSource::new));

    public static void init() {
        DynamicRegistries.registerSynced(REGISTRY_KEY, CODEC);
    }
}
