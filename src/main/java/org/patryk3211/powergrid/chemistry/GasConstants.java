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

import org.patryk3211.powergrid.chemistry.reagent.mixture.VolumeReagentInventory;

public class GasConstants {
    public static final double GAS_CONSTANT = 0.003389; // 340.905; // 8.314*50;
    public static final double HEAT_CAPACITY_RATIO = 1.4; // Assumes a 5 dimensional gas
    public static final double CHOKED_FLOW_RATIO = Math.sqrt(HEAT_CAPACITY_RATIO) * Math.pow(2 / (HEAT_CAPACITY_RATIO + 1), (HEAT_CAPACITY_RATIO + 1) / (2 * (HEAT_CAPACITY_RATIO - 1)));
    public static final double CHOKED_FLOW_LIMIT = Math.pow(2 / (HEAT_CAPACITY_RATIO + 1), HEAT_CAPACITY_RATIO / (HEAT_CAPACITY_RATIO - 1));

    public static final double FLOW_HCR_CONST = (2 * HEAT_CAPACITY_RATIO) / (HEAT_CAPACITY_RATIO - 1);
    public static final double PRESSURE_HCR_CONST = (HEAT_CAPACITY_RATIO - 1) / 2;

    public static double flowRate(VolumeReagentInventory inv, double p0, double p1, double t0, double t1) {
        float sign = 1;
        if(p0 < p1) {
            double p = p0;
            p0 = p1;
            p1 = p;
            double t = t0;
            t0 = t1;
            t1 = t;
            sign = -1;
        }

        final var ratio = p1 / p0;
        double rate;
        if(ratio < CHOKED_FLOW_LIMIT) {
            rate = CHOKED_FLOW_RATIO / Math.sqrt(GAS_CONSTANT * t0);
            return sign * rate * p0 * 0.05f;
        } else {
            var targetPressure = (p0 + p1) * 0.5f;
            var pressureDifference = p0 - targetPressure;
            return inv.nFromPressure(pressureDifference);
        }
    }
}
