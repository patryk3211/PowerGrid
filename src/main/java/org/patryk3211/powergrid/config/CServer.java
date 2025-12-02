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

import net.createmod.catnip.config.ConfigBase;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;

public class CServer extends ConfigBase {
    public final CKinetics kinetics = nested(0, CKinetics::new, Comments.kinetics);
    public final CRecipes recipes = nested(0, CRecipes::new, Comments.recipes);
    public final CElectricity electricity = nested(0, CElectricity::new, Comments.electricity);

    @Override
    public String getName() {
        return "server";
    }

    @Override
    public void onReload() {
        super.onReload();
        PowerGrid.LOGGER.warn("Server config reloaded, this can cause unexpected behaviour if done during gameplay!");
        GlobalElectricNetworks.configsReloaded();
    }

    private static class Comments {
        public static final String electricity = "All things related to purely electrical devices";
        public static final String kinetics = "Things related to kinetic and electrokinetic devices";
        public static final String recipes = "Recipe configuration values";
    }
}
