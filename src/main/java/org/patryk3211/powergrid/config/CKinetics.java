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

public class CKinetics extends ConfigBase {
    public final ConfigFloat encasedFanCoolingStrength = f(0.01f, 0, "encasedFanCooling", Comments.encasedFanCoolingStrength);

    public final ConfigFloat generatorClutchForcePerSegment = f(10f, 0, "generatorSegmentForce", Comments.generatorClutchForcePerSegment);

    public final ConfigFloat lightningAttractorSpeedFactor = f(1 / 20f, 0, "lightningAttractorSpeedFactor", Comments.lightningAttractorSpeedFactor);
    public final ConfigFloat lightningAttractorSailFactor = f(1 / 8f, 0, "lightningAttractorSailFactor", Comments.lightningAttractorSailFactor);
    public final ConfigFloat lightningAttractorMaxFrequency = f(1 / 20f, 0, "lightningAttractorMaxFrequency", Comments.lightningAttractorMaxFrequency);

    public final ConfigInt rotorAssemblyMaxSize = i(8, 3, "rotorAssemblyMaxSize", Comments.rotorAssemblyMaxSize);

    public final ConfigFloat motorRPMPerVolt = f(0.5f, 0, "motorRPMPerVolt", Comments.motorRPMPerVolt);

    public final ConfigInt rotorRPMMax = i(256, 0, "rotorRPMMax", Comments.rotorRPMMax);

    public final CStress stressValues = nested(1, CStress::new, Comments.stress);

    @Override
    public String getName() {
        return "kinetics";
    }

    private static class Comments {
        public static final String encasedFanCoolingStrength = "Cooling multiplier applied to devices in the air stream (multiplied by rotational speed)";
        public static final String generatorClutchForcePerSegment = "Maximum force applied by clutch for each segment of a rotating assembly";
        public static final String lightningAttractorSpeedFactor = "How much lightning rod linear velocity is needed to reach the maximum lightning attractor firing rate";
        public static final String lightningAttractorSailFactor = "How many sail blocks are needed to reach the maximum lightning attractor firing rate";
        public static final String lightningAttractorMaxFrequency = "How often can the lightning attractor fire";
        public static final String rotorAssemblyMaxSize = "Maximum length of a rotor assembly";
        public static final String motorRPMPerVolt = "Rotation speed of the electric motor for every volt across it";
        public static final String rotorRPMMax = "Maximum rotation speed of a rotor";
        
        public static final String stress = "Fine tune the kinetic stats of individual components";
    }
}
