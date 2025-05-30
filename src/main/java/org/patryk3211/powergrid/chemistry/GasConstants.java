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
package org.patryk3211.powergrid.chemistry;

public class GasConstants {
    public static final double GAS_CONSTANT = 0.010167;
    public static final double HEAT_CAPACITY_RATIO = 1.4; // Assumes a 5 dimensional gas

    public static final double PRESSURE_HCR_CONST = (HEAT_CAPACITY_RATIO - 1) / 2;
    public static final double STACK_EFFECT_CONST = 14.25f;

    public static final float ATMOSPHERIC_PRESSURE = 1.013f;
    public static final float ATMOSPHERE_TEMPERATURE = 22f;
    public static final float ATMOSPHERE_ABSOLUTE_TEMPERATURE = ATMOSPHERE_TEMPERATURE + 273.15f;

    public static int calculateMoveAmount(double p0, double p1, float availableVolume, float t0, float t1) {
        int sign = 1;
        if(p0 < p1) {
            sign = -1;
            var p = p0;
            p0 = p1;
            p1 = p;
            t0 = t1;
        }
        var fraction = p0 - p1;
        return (int) (fraction * availableVolume / (GAS_CONSTANT * t0)) * sign;
    }
}
