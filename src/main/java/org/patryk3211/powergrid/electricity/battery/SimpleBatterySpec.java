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
package org.patryk3211.powergrid.electricity.battery;

import java.util.function.Function;

public class SimpleBatterySpec implements BatterySpec {
    public static final SimpleBatterySpec ACID_BATTERY = new SimpleBatterySpec(
            43200, // This should amount to 1Ah at 12V
            38880, // Initial charge level is 90%
            2.0f,
            0.75f,
            e -> 1.2f * e + 11.5f,
            // This resistance makes the battery effectively dead after a deep discharge
            e -> Math.min(10000, (float) Math.exp(-21.18323 * e + 10.58157) + 0.1f)
    );

    private final float maxCharge;
    private final float initialCharge;
    private final float thermalMass;
    private final float dissipationFactor;
    private final Function<Float, Float> voltageFunction;
    private final Function<Float, Float> resistanceFunction;

    public SimpleBatterySpec(float maxCharge, float initialCharge, float thermalMass, float dissipationFactor, Function<Float, Float> voltageFunction, Function<Float, Float> resistanceFunction) {
        this.maxCharge = maxCharge;
        this.initialCharge = initialCharge;
        this.thermalMass = thermalMass;
        this.dissipationFactor = dissipationFactor;
        this.voltageFunction = voltageFunction;
        this.resistanceFunction = resistanceFunction;
    }

    @Override
    public float getMaxCharge() {
        return maxCharge;
    }

    @Override
    public float getInitialCharge() {
        return initialCharge;
    }

    @Override
    public float getThermalMass() {
        return thermalMass;
    }

    @Override
    public float getDissipationFactor() {
        return dissipationFactor;
    }

    @Override
    public float calculateVoltage(float chargeLevel) {
        return voltageFunction.apply(chargeLevel);
    }

    @Override
    public float calculateResistance(float chargeLevel) {
        return resistanceFunction.apply(chargeLevel);
    }
}
