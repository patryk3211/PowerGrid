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

import com.simibubi.create.foundation.config.ConfigBase;

public class CElectricity extends ConfigBase {
    public final ConfigFloat heaterResistance = f(10, 0.1f, "heaterResistance", Comments.heaterResistance);
    public final ConfigFloat heaterFanProcessingSpeedMultiplier = f(0.75f, 0, "heaterFanProcessingSpeedMultiplier", Comments.heaterFanProcessingSpeedMultiplier);

    @Override
    public String getName() {
        return "electricity";
    }

    private static class Comments {
        public static final String heaterResistance = "Heating coil electrical resistance";
        public static final String heaterFanProcessingSpeedMultiplier = "Multiplier of the base fan bulk processing time applied to items processed with the heating coil (lower value means faster processing)";
    }
}
