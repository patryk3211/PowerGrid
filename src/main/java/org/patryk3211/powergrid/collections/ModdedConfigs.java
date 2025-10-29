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

import com.simibubi.create.api.stress.BlockStressValues;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.createmod.catnip.config.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.config.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @see com.simibubi.create.infrastructure.config.AllConfigs
 */
public class ModdedConfigs {
    public static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);

    private static CServer server;
    private static CCommon common;

    public static CServer server() {
        return server;
    }

    public static CCommon common() {
        return common;
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

    @ExpectPlatform
    public static void registerPlatform() {
        throw new AssertionError();
    }

    public static void register() {
        server = register(CServer::new, ModConfig.Type.SERVER);
        common = register(CCommon::new, ModConfig.Type.COMMON);

        registerPlatform();

        CStress stress = server().kinetics.stressValues;
        BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
        BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);

        ResistanceValues.register(server.electricity.resistance);
        ThermalValues.register(server.electricity.thermal);
        WireValues.register(server.electricity.wires);
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
        if(modConfig.getType() == ModConfig.Type.SERVER)
            PowerGrid.LOGGER.warn("Server config reloaded, this can cause unexpected behaviour if done during gameplay!");
    }

    public static boolean logsEnabled() {
        return common.lotsOfLogs.get();
    }
}
