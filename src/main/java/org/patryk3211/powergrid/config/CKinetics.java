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

public class CKinetics extends ConfigBase {
    public final ConfigFloat encasedFanCoolingStrength = f(0.01f, 0, "encasedFanCooling", Comments.encasedFanCoolingStrength);

    public final ConfigFloat generatorClutchForcePerSegment = f(10f, 0, "generatorSegmentForce", Comments.generatorClutchForcePerSegment);

    @Override
    public String getName() {
        return "kinetics";
    }

    private static class Comments {
        public static final String encasedFanCoolingStrength = "Cooling multiplier applied to devices in the air stream (multiplied by rotational speed)";
        public static final String generatorClutchForcePerSegment = "Maximum force applied by clutch for each segment of a rotating assembly";
    }
}
