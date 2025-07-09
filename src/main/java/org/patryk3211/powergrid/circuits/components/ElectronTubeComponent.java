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
package org.patryk3211.powergrid.circuits.components;

import com.google.common.collect.ImmutableCollection;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.special.ElectronTubeWire;

import static org.patryk3211.powergrid.electricity.base.ThermalBehaviour.BASE_TEMPERATURE;

public class ElectronTubeComponent extends OrientableComponent {
    // TODO: Value ranges might need balancing
    public static final FloatProperty TUBE_GAIN = new FloatProperty(PowerGrid.MOD_ID, "tube_gain", 5, 1, 50);
    public static final FloatProperty ANODE_RESISTANCE = new FloatProperty(PowerGrid.MOD_ID, "tube_anode_resistance", 5000, 100, 10000);
    public static final FloatProperty SATURATION_CURRENT = new FloatProperty(PowerGrid.MOD_ID, "tube_saturation_current", 0.01f, 0.0001f, 20);
    public static final FloatProperty HEATER_VOLTAGE = new FloatProperty(PowerGrid.MOD_ID, "tube_heater_voltage", 6f, 1f, 16f);
    public static final CalculatedProperty<Float> HEATER_POWER = new CalculatedProperty<>(PowerGrid.MOD_ID, "tube_heater_power", state -> {
        var Is = state.get(SATURATION_CURRENT);
        return Math.max(5f, Is * 50f);
        }, value -> String.format("%.1f W", value));

    public ElectronTubeComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(TUBE_GAIN, ANODE_RESISTANCE, SATURATION_CURRENT, HEATER_VOLTAGE, HEATER_POWER);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
        var perveance = ElectronTubeWire.calculatePerveance(1,
                placed.get(TUBE_GAIN),
                1 / placed.get(ANODE_RESISTANCE));
        final var saturationCurrent = placed.get(SATURATION_CURRENT);
        var tube = new ElectronTubeWire(
                placed.get(TUBE_GAIN), perveance, saturationCurrent,
                builder.terminalNode(0), // Cathode
                builder.terminalNode(2), // Anode
                builder.terminalNode(1)  // Grid
        );
        builder.add(tube);

        var targetPower = placed.get(HEATER_POWER);
        var heaterCurrent = targetPower / placed.get(HEATER_VOLTAGE);
        var heaterResistance = placed.get(HEATER_VOLTAGE) / heaterCurrent;
        var heater = builder.connect(heaterResistance, builder.terminalNode(0), builder.terminalNode(3));

        placed.add(tube);
        placed.add(heater);

        final var operatingTemperature = 1400f;
        final var dissipationFactor = targetPower / (operatingTemperature - BASE_TEMPERATURE);
        thermals.builder()
                .addHeatSource(heater)
                .setThermalMass(0.001f)
                .setOverheatTemperature(1500f)
                .setDissipationFactor(dissipationFactor)
                .withCallback(temperature -> tube.setSaturationCurrent(
                        MathHelper.clamp(temperature - 1300f, 0, 150) * saturationCurrent / 100
                ));
    }
}
