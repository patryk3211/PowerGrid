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

public class CRecipes extends ConfigBase {
    public final ConfigFloat lightningMagnetizationChance = f(0.05f, 0, 1, "lightningMagnetizationChance", Comments.lightningMagnetizationChance);

    @Override
    public String getName() {
        return "recipes";
    }

    private static class Comments {
        public static final String lightningMagnetizationChance = "Chance for lightning strike to turn a single iron ingot into a magnet";
    }
}
