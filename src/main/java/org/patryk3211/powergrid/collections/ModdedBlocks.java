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

import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.tterrag.registrate.util.entry.BlockEntry;
import org.patryk3211.powergrid.electricity.battery.BatteryBlock;
import org.patryk3211.powergrid.electricity.creative.CreativeResistorBlock;
import org.patryk3211.powergrid.electricity.creative.CreativeSourceBlock;
import org.patryk3211.powergrid.electricity.electricswitch.SwitchBlock;
import org.patryk3211.powergrid.electricity.gauge.CurrentGaugeBlock;
import org.patryk3211.powergrid.electricity.gauge.GaugeBlock;
import org.patryk3211.powergrid.electricity.gauge.VoltageGaugeBlock;
import org.patryk3211.powergrid.electricity.heater.HeaterBlock;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerCoreBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerSmallBlock;
import org.patryk3211.powergrid.electricity.wireconnector.ConnectorBlock;
import org.patryk3211.powergrid.kinetics.basicgenerator.BasicGeneratorBlock;
import org.patryk3211.powergrid.kinetics.generator.coil.CoilBlock;
import org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlock;
import org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlock;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedBlocks {
    public static final BlockEntry<BasicGeneratorBlock> BASIC_GENERATOR = BasicGeneratorBlock.register(REGISTRATE);
    public static final BlockEntry<ConnectorBlock> WIRE_CONNECTOR = ConnectorBlock.register(REGISTRATE);
    public static final BlockEntry<BatteryBlock> BATTERY = BatteryBlock.register(REGISTRATE);
    public static final BlockEntry<HeaterBlock> HEATING_COIL = HeaterBlock.register(REGISTRATE);

    public static final BlockEntry<VoltageGaugeBlock> ANDESITE_VOLTAGE_METER = REGISTRATE.block("andesite_voltage_gauge", VoltageGaugeBlock::new)
            .transform(GaugeBlock.setMaxValue(20))
            .transform(GaugeBlock.setMaterial(GaugeBlock.Material.ANDESITE))
            .simpleItem()
            .register();
    public static final BlockEntry<VoltageGaugeBlock> BRASS_VOLTAGE_METER = REGISTRATE.block("brass_voltage_gauge", VoltageGaugeBlock::new)
            .transform(GaugeBlock.setMaxValue(200))
            .transform(GaugeBlock.setMaterial(GaugeBlock.Material.BRASS))
            .simpleItem()
            .register();

    public static final BlockEntry<CurrentGaugeBlock> ANDESITE_CURRENT_METER = REGISTRATE.block("andesite_current_gauge", CurrentGaugeBlock::new)
            .transform(GaugeBlock.setMaxValue(5))
            .transform(GaugeBlock.setMaterial(GaugeBlock.Material.ANDESITE))
            .transform(CurrentGaugeBlock.setResistance(0.25f))
            .simpleItem()
            .register();
    public static final BlockEntry<CurrentGaugeBlock> BRASS_CURRENT_METER = REGISTRATE.block("brass_current_gauge", CurrentGaugeBlock::new)
            .transform(GaugeBlock.setMaxValue(25))
            .transform(GaugeBlock.setMaterial(GaugeBlock.Material.BRASS))
            .transform(CurrentGaugeBlock.setResistance(0.05f))
            .simpleItem()
            .register();

    public static final BlockEntry<RotorBlock> ROTOR = REGISTRATE.block("rotor", RotorBlock::new)
            .properties(settings -> settings.nonOpaque())
            .transform(BlockStressDefaults.setImpact(4))
            .simpleItem()
            .register();

    public static final BlockEntry<CoilBlock> COIL = REGISTRATE.block("coil", CoilBlock::new)
            .simpleItem()
            .register();

    public static final BlockEntry<GeneratorHousing> GENERATOR_HOUSING = REGISTRATE.block("generator_housing", GeneratorHousing::new)
            .simpleItem()
            .register();

    public static final BlockEntry<SwitchBlock> SWITCH = REGISTRATE.block("switch", SwitchBlock::new)
            .simpleItem()
            .register();

    public static final BlockEntry<CreativeSourceBlock> CREATIVE_VOLTAGE_SOURCE = REGISTRATE.block("creative_voltage_source", CreativeSourceBlock::new)
            .simpleItem()
            .register();
    public static final BlockEntry<CreativeSourceBlock> CREATIVE_CURRENT_SOURCE = REGISTRATE.block("creative_current_source", CreativeSourceBlock::new)
            .simpleItem()
            .register();
    public static final BlockEntry<CreativeResistorBlock> CREATIVE_RESISTOR = REGISTRATE.block("creative_resistor", CreativeResistorBlock::new)
            .simpleItem()
            .register();

    public static final BlockEntry<LightFixtureBlock> LIGHT_FIXTURE = REGISTRATE.block("light_fixture", LightFixtureBlock::new)
            .transform(LightFixtureBlock.setBulbModelOffset(0, 0.125f, 0))
            .simpleItem()
            .register();

    public static final BlockEntry<TransformerCoreBlock> TRANSFORMER_CORE = REGISTRATE.block("transformer_core", TransformerCoreBlock::new)
            .simpleItem()
            .register();
    public static final BlockEntry<TransformerSmallBlock> TRANSFORMER_SMALL = REGISTRATE.block("transformer_small", TransformerSmallBlock::new)
            .register();
    public static final BlockEntry<TransformerMediumBlock> TRANSFORMER_MEDIUM = REGISTRATE.block("transformer_medium", TransformerMediumBlock::new)
            .register();

    public static final BlockEntry<ElectricMotorBlock> ELECTRIC_MOTOR = REGISTRATE.block("electric_motor", ElectricMotorBlock::new)
            .transform(BlockStressDefaults.setCapacity(64))
            .simpleItem()
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
