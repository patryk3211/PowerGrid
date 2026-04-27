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

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.createmod.catnip.config.ConfigBase;
import net.minecraftforge.fml.config.ModConfig;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;

import java.util.Map;

import static org.patryk3211.powergrid.collections.ModdedConfigs.CONFIGS;

public class ModdedConfigsImpl {
    public static void registerPlatform() {
        for(Map.Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet())
            ForgeConfigRegistry.INSTANCE.register(PowerGrid.MOD_ID, pair.getKey(), pair.getValue().specification);

        ModConfigEvents.loading(PowerGrid.MOD_ID).register(ModdedConfigs::onLoad);
        ModConfigEvents.reloading(PowerGrid.MOD_ID).register(ModdedConfigs::onReload);
    }
}
