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

import com.simibubi.create.content.kinetics.base.HalfShaftInstance;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlockEntity;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatRenderer;
import org.patryk3211.powergrid.electricity.battery.BatteryBlockEntity;
import org.patryk3211.powergrid.electricity.creative.CreativeResistorBlockEntity;
import org.patryk3211.powergrid.electricity.creative.CreativeSourceBlockEntity;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchBlockEntity;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchInstance;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchRenderer;
import org.patryk3211.powergrid.electricity.electricswitch.SwitchBlockEntity;
import org.patryk3211.powergrid.electricity.electrode.VatElectrodeBlockEntity;
import org.patryk3211.powergrid.electricity.electromagnet.ElectromagnetBlockEntity;
import org.patryk3211.powergrid.electricity.fan.ElectricFanBlockEntity;
import org.patryk3211.powergrid.electricity.fan.ElectricFanRenderer;
import org.patryk3211.powergrid.electricity.gauge.CurrentGaugeBlockEntity;
import org.patryk3211.powergrid.electricity.gauge.GaugeRenderer;
import org.patryk3211.powergrid.electricity.gauge.VoltageGaugeBlockEntity;
import org.patryk3211.powergrid.electricity.heater.HeaterBlockEntity;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlockEntity;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureRenderer;
import org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlockEntity;
import org.patryk3211.powergrid.electricity.transformer.TransformerSmallBlockEntity;
import org.patryk3211.powergrid.electricity.wireconnector.ConnectorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.coil.CoilBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorRenderer;
import org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlockEntity;
import org.patryk3211.powergrid.kinetics.motor.ElectricMotorRenderer;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedBlockEntities {
    public static final BlockEntityEntry<ConnectorBlockEntity> WIRE_CONNECTOR =
            REGISTRATE.blockEntity("wire_connector", ConnectorBlockEntity::new)
                    .validBlocks(ModdedBlocks.WIRE_CONNECTOR, ModdedBlocks.HEAVY_WIRE_CONNECTOR)
                    .register();

    public static final BlockEntityEntry<BatteryBlockEntity> BATTERY =
            REGISTRATE.blockEntity("battery", BatteryBlockEntity::new)
                    .validBlock(ModdedBlocks.BATTERY)
                    .register();

    public static final BlockEntityEntry<VoltageGaugeBlockEntity> VOLTAGE_METER =
            REGISTRATE.blockEntity("voltage_meter", VoltageGaugeBlockEntity::new)
                    .validBlocks(ModdedBlocks.ANDESITE_VOLTAGE_METER, ModdedBlocks.BRASS_VOLTAGE_METER)
                    .renderer(() -> GaugeRenderer::new)
                    .register();

    public static final BlockEntityEntry<CurrentGaugeBlockEntity> CURRENT_METER =
            REGISTRATE.blockEntity("current_meter", CurrentGaugeBlockEntity::new)
                    .validBlocks(ModdedBlocks.ANDESITE_CURRENT_METER, ModdedBlocks.BRASS_CURRENT_METER)
                    .renderer(() -> GaugeRenderer::new)
                    .register();

    public static final BlockEntityEntry<HeaterBlockEntity> HEATING_COIL =
            REGISTRATE.blockEntity("heating_coil", HeaterBlockEntity::new)
                    .validBlock(ModdedBlocks.HEATING_COIL)
                    .register();

    public static final BlockEntityEntry<RotorBlockEntity> GENERATOR_ROTOR =
            REGISTRATE.blockEntity("generator_rotor", RotorBlockEntity::new)
                    .validBlock(ModdedBlocks.GENERATOR_ROTOR)
                    .renderer(() -> RotorRenderer::new)
                    .register();
    public static final BlockEntityEntry<CoilBlockEntity> GENERATOR_COIL =
            REGISTRATE.blockEntity("generator_coil", CoilBlockEntity::new)
                    .validBlock(ModdedBlocks.GENERATOR_COIL)
                    .register();

    public static final BlockEntityEntry<SwitchBlockEntity> SWITCH =
            REGISTRATE.blockEntity("switch", SwitchBlockEntity::new)
                    .validBlocks(ModdedBlocks.LV_SWITCH, ModdedBlocks.MV_SWITCH)
                    .register();

    public static final BlockEntityEntry<HvSwitchBlockEntity> HV_SWITCH =
            REGISTRATE.blockEntity("hv_switch", HvSwitchBlockEntity::new)
                    .instance(() -> HvSwitchInstance::new)
                    .validBlock(ModdedBlocks.HV_SWITCH)
                    .renderer(() -> HvSwitchRenderer::new)
                    .register();

    public static final BlockEntityEntry<CreativeSourceBlockEntity> CREATIVE_SOURCE =
            REGISTRATE.blockEntity("creative_source", CreativeSourceBlockEntity::new)
                    .validBlocks(ModdedBlocks.CREATIVE_VOLTAGE_SOURCE, ModdedBlocks.CREATIVE_CURRENT_SOURCE)
                    .register();
    public static final BlockEntityEntry<CreativeResistorBlockEntity> CREATIVE_RESISTOR =
            REGISTRATE.blockEntity("creative_resistor", CreativeResistorBlockEntity::new)
                    .validBlock(ModdedBlocks.CREATIVE_RESISTOR)
                    .register();

    public static final BlockEntityEntry<LightFixtureBlockEntity> LIGHT_FIXTURE =
            REGISTRATE.blockEntity("light_fixture", LightFixtureBlockEntity::new)
                    .validBlock(ModdedBlocks.LIGHT_FIXTURE)
                    .renderer(() -> LightFixtureRenderer::new)
                    .register();

    public static final BlockEntityEntry<TransformerSmallBlockEntity> TRANSFORMER_SMALL =
            REGISTRATE.blockEntity("transformer_small", TransformerSmallBlockEntity::new)
                    .validBlock(ModdedBlocks.TRANSFORMER_SMALL)
                    .register();

    public static final BlockEntityEntry<TransformerMediumBlockEntity> TRANSFORMER_MEDIUM =
            REGISTRATE.blockEntity("transformer_medium", TransformerMediumBlockEntity::new)
                    .validBlock(ModdedBlocks.TRANSFORMER_MEDIUM)
                    .register();

    public static final BlockEntityEntry<ElectricMotorBlockEntity> ELECTRIC_MOTOR =
            REGISTRATE.blockEntity("electric_motor", ElectricMotorBlockEntity::new)
                    .instance(() -> HalfShaftInstance::new)
                    .validBlock(ModdedBlocks.ELECTRIC_MOTOR)
                    .renderer(() -> ElectricMotorRenderer::new)
                    .register();

    public static final BlockEntityEntry<ElectromagnetBlockEntity> ELECTROMAGNET =
            REGISTRATE.blockEntity("electromagnet", ElectromagnetBlockEntity::new)
                    .validBlock(ModdedBlocks.ELECTROMAGNET)
                    .register();

    public static final BlockEntityEntry<ChemicalVatBlockEntity> CHEMICAL_VAT =
            REGISTRATE.blockEntity("chemical_vat", ChemicalVatBlockEntity::new)
                    .validBlock(ModdedBlocks.CHEMICAL_VAT)
                    .renderer(() -> ChemicalVatRenderer::new)
                    .register();

    public static final BlockEntityEntry<ElectricFanBlockEntity> ELECTRIC_FAN =
            REGISTRATE.blockEntity("electric_fan", ElectricFanBlockEntity::new)
                    .validBlock(ModdedBlocks.ELECTRIC_FAN)
                    .renderer(() -> ElectricFanRenderer::new)
                    .register();

    public static final BlockEntityEntry<VatElectrodeBlockEntity> VAT_ELECTRODE =
            REGISTRATE.blockEntity("vat_electrode", VatElectrodeBlockEntity::new)
                    .validBlock(ModdedBlocks.VAT_ELECTRODE)
                    .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
