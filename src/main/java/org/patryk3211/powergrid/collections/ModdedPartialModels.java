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
package org.patryk3211.powergrid.collections;

import com.jozufozu.flywheel.core.PartialModel;
import org.patryk3211.powergrid.PowerGrid;

public class ModdedPartialModels {
    public static final PartialModel ANDESITE_VOLTAGE_HEAD = block("gauge/andesite/voltage_head");
    public static final PartialModel BRASS_VOLTAGE_HEAD = block("gauge/brass/voltage_head");
    public static final PartialModel ANDESITE_CURRENT_HEAD = block("gauge/andesite/current_head");
    public static final PartialModel BRASS_CURRENT_HEAD = block("gauge/brass/current_head");
    public static final PartialModel BRASS_GAUGE_DIAL = block("gauge/brass/dial");
    public static final PartialModel SHAFT_BIT = block("shaft_bit");
    public static final PartialModel ROTOR_FULL = block("rotor/rotor_none");
    public static final PartialModel ROTOR_SHAFT = block("rotor/rotor_shaft");

    public static final PartialModel LIGHT_BULB_OFF = block("lamps/light_bulb");
    public static final PartialModel LIGHT_BULB_ON = block("lamps/light_bulb_on");
    public static final PartialModel LIGHT_BULB_BROKEN = block("lamps/light_bulb_broken");

    public static final PartialModel GROWTH_LAMP_OFF = block("lamps/growth_lamp");
    public static final PartialModel GROWTH_LAMP_ON = block("lamps/growth_lamp_on");
    public static final PartialModel GROWTH_LAMP_BROKEN = block("lamps/growth_lamp_broken");

    public static final PartialModel HV_SWITCH_ROD = block("switches/hv_switch_rod");

    private static PartialModel block(String path) {
        return new PartialModel(PowerGrid.asResource("block/" + path));
    }

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
