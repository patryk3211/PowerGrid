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
    public final ConfigFloat heaterSmokingPower = f(20, 0, "heaterSmokingPower", Comments.heaterSmokingPower);
    public final ConfigFloat heaterBlastingPower = f(100, 0, "heaterBlastingPower", Comments.heaterBlastingPower);

    @Override
    public String getName() {
        return "electricity";
    }

    private static class Comments {
        public static final String heaterSmokingPower = "Minimum power required for heating coil to enable bulk smoking";
        public static final String heaterBlastingPower = "Minimum power required for heating coil to enable bulk blasting";
    }
}
