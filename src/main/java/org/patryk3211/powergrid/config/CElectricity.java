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

public class CElectricity extends ConfigBase {
    public final ConfigBool explosiveDeconstruction = b(true, "explosiveDeconstruction", Comments.explosiveDeconstruction);
    public final ConfigBool overheating = b(true, "overheating", Comments.overheating);
    public final ConfigBool wireOverheating = b(true, "wireOverheating", Comments.wireOverheating);

    public final ConfigFloat heaterFanProcessingSpeedMultiplier = f(0.75f, 0, "heaterFanProcessingSpeedMultiplier", Comments.heaterFanProcessingSpeedMultiplier);

    public final ConfigInt growthLampRadius = i(2, 1, "growthLampRadius", Comments.growthLampRadius);
    public final ConfigInt growthLampChance = i(50, 0, "growthLampChance", Comments.growthLampChance);

    public final ConfigFloat forgeEnergyPerVolt = f(2, 0, "forgeEnergyPerVolt", Comments.forgeEnergyPerVolt);
    public final ConfigFloat forgeEnergyPerWatt = f(10, 0, "forgeEnergyPerWatt", Comments.forgeEnergyPerWatt);
    public final ConfigInt tfmgConnectorPower = i(10000, 0, "TFMGConnectorPower", Comments.tfmgConnectorPower);

    public final ConfigFloat acidBatteryInitialCharge = f(0.9f, 0, 1.0f, "acidBatteryInitialCharge", Comments.acidBatteryInitialCharge);

    public final ConfigFloat transformerMutualInductanceMultiplier = f(10, 1, "transformerMutualInductanceMultiplier", Comments.transformerMutualInductanceMultiplier);

    public final ConfigFloat holdingCurrentPercent = f(0.9f, 0.01f, 0.99f, "holdingCurrentPercent", Comments.holdingCurrentPercent);

    public final ConfigInt groundingMinimumBlocks = i(10, 0, "groundingMinimumBlocks", Comments.groundingMinimumBlocks);
    public final ConfigInt groundingMaximumBlocks = i(64, 0, "groundingMaximumBlocks", Comments.groundingMaximumBlocks);
    public final ConfigFloat groundingHighestResistance = f(5000, 0.001f, "groundingHighestResistance", Comments.groundingHighestResistance);
    public final ConfigFloat groundingLowestResistance = f(1, 0.001f, "groundingLowestResistance", Comments.groundingLowestResistance);

    public final ConfigInt carbonPileMaxHeight = i(5, 1, "carbonPileMaxHeight", Comments.carbonPileMaxHeight);
    public final ConfigFloat carbonPileGain = f(10, 0, "carbonPileGain", Comments.carbonPileGain);

    public final ConfigFloat smallCoreAl = f(1.5f, 0, "smallCoreAl", Comments.smallCoreAl);
    public final ConfigFloat smallCoreK = f(0.9999f, 0, 1, "smallCoreK", Comments.smallCoreK);
    public final ConfigFloat mediumCoreAl = f(10.0f, 0, "mediumCoreAl", Comments.mediumCoreAl);
    public final ConfigFloat mediumCoreK = f(0.99999f, 0, 1, "mediumCoreK", Comments.mediumCoreK);


    public final CSolver solver = nested(1, CSolver::new, Comments.solver);
    public final CResistance resistance = nested(1, CResistance::new, Comments.resistance);
    public final CThermal thermal = nested(1, CThermal::new, Comments.thermal);
    public final CWire wires = nested(1, CWire::new, Comments.wires);

    @Override
    public String getName() {
        return "electricity";
    }

    private static class Comments {
        public static final String solver = "Things related to how the simulation is handled (probably shouldn't be altered unless you know what you are doing)";
        public static final String resistance = "Resistance values for all devices";
        public static final String thermal = "Thermal properties for all devices";
        public static final String wires = "Configuration of server-side wire properties";

        public static final String explosiveDeconstruction = "Controls the behaviour of overheated devices. If false, instead of exploding, they break without dropping items.";
        public static final String overheating = "Controls the overheat mechanic. Devices which are overheated, break.";
        public static final String wireOverheating = "Controls the overheat mechanic for wires. Wires will burn if they overheat.";

        public static final String heaterFanProcessingSpeedMultiplier = "Multiplier of the base fan bulk processing time applied to items processed with the heating coil (lower value means faster processing)";

        public static final String growthLampRadius = "Radius of the area affected by growth lamp effect";
        public static final String growthLampChance = "Chance value for the growth lamp to tick a random block in its area (lower value = higher chance), this value is divided by lamp's power level";

        public static final String forgeEnergyPerVolt = "Conversion rate of volts to Forge Energy (controls buffer size and max transfer rate)";
        public static final String forgeEnergyPerWatt = "Conversion rate of watts to Forge Energy (controls the actual energy cost of a unit of FE)";
        public static final String tfmgConnectorPower = "Maximum power generated by a device connector when it is connected to a 'Create: The Factory Must Grow' electrical network";

        public static final String acidBatteryInitialCharge = "Initial charge of the acid battery";

        public static final String transformerMutualInductanceMultiplier = "Multiplies the mutual inductance of transformers to get a resistance. Bigger values make transformers use less electricity.";

        public static final String holdingCurrentPercent = "Percent of the turn-on current required for on state to be kept by relays and conductors";

        public static final String groundingMinimumBlocks = "Minimum blocks needed for a grounding rod to work";
        public static final String groundingMaximumBlocks = "Maximum conductive blocks needed for a grounding rod to reach its lowest resistance";
        public static final String groundingHighestResistance = "Highest resistance of the grounding rod";
        public static final String groundingLowestResistance = "Lowest resistance of the grounding rod";

        public static final String carbonPileMaxHeight = "Maximum block height of the carbon pile structure";
        public static final String carbonPileGain = "Carbon pile \"gain\", controls how much the coil current affects the resistance";

        public static final String smallCoreAl = "Al parameter of small transformer core (affects resistance per turn)";
        public static final String smallCoreK = "K parameter of small transformer core (affects stray resistance)";
        public static final String mediumCoreAl = "Al parameter of medium transformer core (affects resistance per turn)";
        public static final String mediumCoreK = "K parameter of medium transformer core (affects stray resistance)";
    }
}
