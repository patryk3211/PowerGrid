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

import com.simibubi.create.foundation.config.ConfigBase;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.config.CServer;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @see com.simibubi.create.infrastructure.config.AllConfigs
 */
public class ModdedConfigs {
    private static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);

    private static CServer server;

    public static CServer server() {
        return server;
    }

    public static ConfigBase byType(ModConfig.Type type) {
        return CONFIGS.get(type);
    }

    private static <T extends ConfigBase> T register(Supplier<T> factory, ModConfig.Type side) {
        Pair<T, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        T config = specPair.getLeft();
        config.specification = specPair.getRight();
        CONFIGS.put(side, config);
        return config;
    }

    public static void register() {
        server = register(CServer::new, ModConfig.Type.SERVER);

        for(Map.Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet())
            ForgeConfigRegistry.INSTANCE.register(PowerGrid.MOD_ID, pair.getKey(), pair.getValue().specification);

        ModConfigEvents.loading(PowerGrid.MOD_ID).register(ModdedConfigs::onLoad);
        ModConfigEvents.reloading(PowerGrid.MOD_ID).register(ModdedConfigs::onReload);
    }

    public static void onLoad(ModConfig modConfig) {
        for(ConfigBase config : CONFIGS.values())
            if(config.specification == modConfig.getSpec())
                config.onLoad();
    }

    public static void onReload(ModConfig modConfig) {
        for(ConfigBase config : CONFIGS.values())
            if(config.specification == modConfig.getSpec())
                config.onReload();
        PowerGrid.LOGGER.warn("Config reloaded, this can cause unexpected behaviour!");
    }
}
