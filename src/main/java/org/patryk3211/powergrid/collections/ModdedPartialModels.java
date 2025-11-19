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

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import org.patryk3211.powergrid.PowerGrid;

public class ModdedPartialModels {
    public static final PartialModel SHAFT_BIT = block("shaft_bit");

    public static final PartialModel CONDUCTIVE_VOLTAGE_HEAD = block("gauge/conductive/voltage_head");
    public static final PartialModel CONDUCTIVE_CURRENT_HEAD = block("gauge/conductive/current_head");
    public static final PartialModel CONDUCTIVE_POWER_HEAD = block("gauge/conductive/power_head");

    public static final PartialModel COMMUTATOR_SHAFT = block("generator/commutator_shaft");
    public static final PartialModel COMMUTATOR_BRUSH = block("generator/commutator_brush");
    public static final PartialModel VERTICAL_COMMUTATOR_BRUSH = block("generator/vertical_commutator_brush");

    public static final PartialModel CLUTCH_SHAFT = block("generator/clutch_shaft");

    public static final PartialModel LIGHT_BULB_OFF = block("lamps/light_bulb");
    public static final PartialModel LIGHT_BULB_ON = block("lamps/light_bulb_on");
    public static final PartialModel LIGHT_BULB_BROKEN = block("lamps/light_bulb_broken");
    public static final PartialModel LIGHT_BULB_LIGHT = block("lamps/light_bulb_light");

    public static final PartialModel GROWTH_LAMP_OFF = block("lamps/growth_lamp");
    public static final PartialModel GROWTH_LAMP_ON = block("lamps/growth_lamp_on");
    public static final PartialModel GROWTH_LAMP_BROKEN = block("lamps/growth_lamp_broken");
    public static final PartialModel GROWTH_LAMP_LIGHT = block("lamps/growth_lamp_light");

    public static final PartialModel HV_SWITCH_ROD = block("switches/hv_switch_rod");
    public static final PartialModel VARIAC_ARMATURE = block("variac/armature");

    public static final PartialModel SPARK_GAP_ARM = block("spark_gap/arm");

    public static final PartialModel ELECTRON_TUBE_GLOW = model("component/electron_tube_glow");
    public static final PartialModel REGULATOR_TUBE_GLOW = model("component/regulator_tube_glow");
    public static final PartialModel POTENTIOMETER_KNOB = model("component/potentiometer_knob");

    public static final PartialModel NEON_TUBE_BULB = model("component/neon_bulb_bulb");
    public static final PartialModel NEON_TUBE_GLOW = model("component/neon_bulb_glow");

    public static final PartialModel ROTOR = block("generator/rotor");
    public static final PartialModel INDUCTION_ROTOR = block("generator/induction_rotor");

    public static final PartialModel THERMOMETER_NEEDLE = block("thermometer/needle");
    public static final PartialModel THERMOMETER_NEEDLE_RED = block("thermometer/needle_red");

    public static final PartialModel MULTIMETER_NEEDLE = model("item/multimeter/indicator");
    public static final PartialModel COMPONENT_GAUGE_NEEDLE = model("component/gauge_needle");

    public static final PartialModel PLUG = block("plug");

    public static final PartialModel PLOTTER_POINTER = block("plotter/pointer");
    public static final PartialModel PLOTTER_PAPER = block("plotter/paper");
    public static final PartialModel CRT_BACKGROUND = block("crt_background");

    public static final PartialModel HV_BREAKER_SIGNAL1 = block("switches/hv_breaker_signal1");
    public static final PartialModel HV_BREAKER_SIGNAL2 = block("switches/hv_breaker_signal2");

    public static final SpriteShiftEntry PAPER_SHIFT = SpriteShifter.get(
            PowerGrid.asResource("block/plotter_paper"),
            PowerGrid.asResource("block/plotter_paper_shift"));

    private static PartialModel block(String path) {
        return PartialModel.of(PowerGrid.asResource("block/" + path));
    }

    private static PartialModel model(String path) {
        return PartialModel.of(PowerGrid.asResource(path));
    }

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
