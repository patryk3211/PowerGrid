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
package org.patryk3211.powergrid.config;

import dev.architectury.platform.Platform;
import net.createmod.catnip.config.ConfigBase;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;

public class CServer extends ConfigBase {
    public static final int CONFIG_VERSION = 5;
    public final ConfigInt version = i(CONFIG_VERSION, "configVersion", Comments.version);

    public final CColdSweat coldSweat = (Platform.isModLoaded("cold_sweat")) ? nested(0, CColdSweat::new, Comments.coldSweat) : null;
    public final CKinetics kinetics = nested(0, CKinetics::new, Comments.kinetics);
    public final CRecipes recipes = nested(0, CRecipes::new, Comments.recipes);
    public final CEquipment equipment = nested(0, CEquipment::new, Comments.equipment);
    public final CElectricity electricity = nested(0, CElectricity::new, Comments.electricity);

    @Override
    public String getName() {
        return "server";
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(!isUpToDate())
            PowerGrid.LOGGER.warn("Detected outdated configs, consider resetting your server configs if you experience issues.");
    }

    @Override
    public void onReload() {
        super.onReload();
        PowerGrid.LOGGER.warn("Server config reloaded, this can cause unexpected behaviour if done during gameplay!");
        GlobalElectricNetworks.configsReloaded();
    }

    public boolean isUpToDate() {
        if(version.get() < 0)
            return true;
        return version.get() >= CONFIG_VERSION;
    }

    private static class Comments {
        public static final String electricity = "All things related to purely electrical devices";
        public static final String kinetics = "Things related to kinetic and electrokinetic devices";
        public static final String recipes = "Recipe configuration values";
        public static final String coldSweat = "Cold Sweat configuration values";
        public static final String equipment = "Equipment configuration values";
        public static final String version = "Config version check, values below 0 will disable config version checker";
    }
}
