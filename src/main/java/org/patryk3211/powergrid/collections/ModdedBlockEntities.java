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

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardRenderer;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableBlockEntity;
import org.patryk3211.powergrid.electricity.basinheater.BasinHeaterBlockEntity;
import org.patryk3211.powergrid.electricity.battery.MultiBlockBatteryEntity;
import org.patryk3211.powergrid.electricity.battery.PotatoBatteryBlockEntity;
import org.patryk3211.powergrid.electricity.bell.AlarmBellBlockEntity;
import org.patryk3211.powergrid.electricity.contactor.ContactorBlockEntity;
import org.patryk3211.powergrid.electricity.creative.CreativeResistorBlockEntity;
import org.patryk3211.powergrid.electricity.creative.CreativeSourceBlockEntity;
import org.patryk3211.powergrid.electricity.deviceconnector.DeviceConnectorBlockEntity;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchBlockEntity;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchRenderer;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchVisual;
import org.patryk3211.powergrid.electricity.electricswitch.SwitchBlockEntity;
import org.patryk3211.powergrid.electricity.electromagnet.ElectromagnetBlockEntity;
import org.patryk3211.powergrid.electricity.fan.ElectricFanBlockEntity;
import org.patryk3211.powergrid.electricity.fan.ElectricFanRenderer;
import org.patryk3211.powergrid.electricity.fuse.FuseHolderBlockEntity;
import org.patryk3211.powergrid.electricity.gauge.CurrentGaugeBlockEntity;
import org.patryk3211.powergrid.electricity.gauge.GaugeRenderer;
import org.patryk3211.powergrid.electricity.gauge.PowerGaugeBlockEntity;
import org.patryk3211.powergrid.electricity.gauge.VoltageGaugeBlockEntity;
import org.patryk3211.powergrid.electricity.grounding.GroundingRodBlockEntity;
import org.patryk3211.powergrid.electricity.heater.HeaterBlockEntity;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlockEntity;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureRenderer;
import org.patryk3211.powergrid.electricity.resistor.ResistorBlockEntity;
import org.patryk3211.powergrid.electricity.socket.SocketBlockEntity;
import org.patryk3211.powergrid.electricity.sparkgap.SparkGapBlockEntity;
import org.patryk3211.powergrid.electricity.sparkgap.SparkGapRenderer;
import org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlockEntity;
import org.patryk3211.powergrid.electricity.transformer.TransformerSmallBlockEntity;
import org.patryk3211.powergrid.electricity.wireconnector.ConnectorBlockEntity;
import org.patryk3211.powergrid.electricity.wireconnector.CordJunctionBlockEntity;
import org.patryk3211.powergrid.equipment.portablebattery.PortableBatteryBlockEntity;
import org.patryk3211.powergrid.equipment.thermometer.ThermometerBlockEntity;
import org.patryk3211.powergrid.equipment.thermometer.ThermometerRenderer;
import org.patryk3211.powergrid.kinetics.base.HalfShaftVisual;
import org.patryk3211.powergrid.kinetics.base.TunedBlockRenderer;
import org.patryk3211.powergrid.kinetics.base.TunedBlockVisual;
import org.patryk3211.powergrid.kinetics.generator.clutch.GeneratorClutchBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.clutch.GeneratorClutchRenderer;
import org.patryk3211.powergrid.kinetics.generator.clutch.GeneratorClutchVisual;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.CommutatorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.CommutatorRenderer;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.CommutatorVisual;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.InductionRotorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorRenderer;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorVisual;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlockEntity;
import org.patryk3211.powergrid.kinetics.motor.ConstantSpeedMotorBlockEntity;
import org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlockEntity;
import org.patryk3211.powergrid.kinetics.motor.ElectricMotorRenderer;
import org.patryk3211.powergrid.kinetics.rheostat.RheostatBlockEntity;
import org.patryk3211.powergrid.kinetics.servo.ServoBlockEntity;
import org.patryk3211.powergrid.kinetics.servo.ServoRenderer;
import org.patryk3211.powergrid.kinetics.variac.VariacBlockEntity;
import org.patryk3211.powergrid.utility.proxy.SubstituteBlockEntityProvider;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedBlockEntities {
    public static final BlockEntityEntry<ConnectorBlockEntity> WIRE_CONNECTOR =
            REGISTRATE.blockEntity("wire_connector", ConnectorBlockEntity::new)
                    .validBlocks(ModdedBlocks.WIRE_CONNECTOR, ModdedBlocks.HEAVY_WIRE_CONNECTOR)
                    .register();

    public static final BlockEntityEntry<CordJunctionBlockEntity> CORD_JUNCTION =
            REGISTRATE.blockEntity("cord_junction", CordJunctionBlockEntity::new)
                    .validBlock(ModdedBlocks.CORD_JUNCTION)
                    .register();

    public static final BlockEntityEntry<MultiBlockBatteryEntity> MULTIBLOCK_BATTERY =
            REGISTRATE.blockEntity("battery", MultiBlockBatteryEntity::new)
                    .validBlock(ModdedBlocks.BATTERY)
                    .register();

    public static final BlockEntityEntry<PotatoBatteryBlockEntity> POTATO_BATTERY =
            REGISTRATE.blockEntity("potato_battery", PotatoBatteryBlockEntity::new)
                    .validBlock(ModdedBlocks.POTATO_BATTERY)
                    .register();

    public static final BlockEntityEntry<VoltageGaugeBlockEntity> VOLTAGE_METER =
            REGISTRATE.blockEntity("voltage_meter", VoltageGaugeBlockEntity::new)
                    .validBlocks(ModdedBlocks.VOLTAGE_METER)
                    .renderer(() -> GaugeRenderer::new)
                    .register();

    public static final BlockEntityEntry<CurrentGaugeBlockEntity> CURRENT_METER =
            REGISTRATE.blockEntity("current_meter", CurrentGaugeBlockEntity::new)
                    .validBlocks(ModdedBlocks.CURRENT_METER)
                    .renderer(() -> GaugeRenderer::new)
                    .register();

    public static final BlockEntityEntry<PowerGaugeBlockEntity> POWER_METER =
            REGISTRATE.blockEntity("power_meter", PowerGaugeBlockEntity::new)
                    .validBlocks(ModdedBlocks.POWER_METER)
                    .renderer(() -> GaugeRenderer::new)
                    .register();

    public static final BlockEntityEntry<HeaterBlockEntity> HEATING_COIL =
            REGISTRATE.blockEntity("heating_coil", HeaterBlockEntity::new)
                    .validBlock(ModdedBlocks.HEATING_COIL)
                    .register();

    public static final BlockEntityEntry<BasinHeaterBlockEntity> BASIN_HEATER =
            REGISTRATE.blockEntity("basin_heater", BasinHeaterBlockEntity::new)
                    .validBlock(ModdedBlocks.BASIN_HEATER)
                    .register();

//    public static final BlockEntityEntry<SimpleRotorBlockEntity> GENERATOR_ROTOR =
//            REGISTRATE.blockEntity("generator_rotor", SimpleRotorBlockEntity::new)
//                    .visual(() -> RotorVisual.of(ModdedPartialModels.ROTOR))
//                    .validBlock(ModdedBlocks.GENERATOR_ROTOR)
//                    .renderer(() -> RotorRenderer::new)
//                    .register();

    public static final BlockEntityEntry<InductionRotorBlockEntity> GENERATOR_INDUCTION_ROTOR =
            REGISTRATE.blockEntity("generator_induction_rotor", InductionRotorBlockEntity::new)
                    .visual(() -> RotorVisual.of(ModdedPartialModels.INDUCTION_ROTOR))
                    .validBlock(ModdedBlocks.GENERATOR_INDUCTION_ROTOR)
                    .renderer(() -> RotorRenderer::new)
                    .register();

    public static final BlockEntityEntry<CommutatorBlockEntity> GENERATOR_COMMUTATOR =
            REGISTRATE.blockEntity("generator_commutator", CommutatorBlockEntity::new)
                    .visual(() -> CommutatorVisual::new)
                    .validBlocks(ModdedBlocks.GENERATOR_COMMUTATOR, ModdedBlocks.GENERATOR_VERTICAL_COMMUTATOR)
                    .renderer(() -> CommutatorRenderer::new)
                    .register();

    public static final BlockEntityEntry<GeneratorClutchBlockEntity> GENERATOR_CLUTCH =
            REGISTRATE.blockEntity("generator_clutch", GeneratorClutchBlockEntity::new)
                    .visual(() -> GeneratorClutchVisual::new)
                    .validBlock(ModdedBlocks.GENERATOR_CLUTCH)
                    .renderer(() -> GeneratorClutchRenderer::new)
                    .register();

    public static final BlockEntityEntry<SwitchBlockEntity> SWITCH =
            REGISTRATE.blockEntity("switch", SwitchBlockEntity::new)
                    .validBlocks(ModdedBlocks.LV_SWITCH, ModdedBlocks.MV_SWITCH, ModdedBlocks.LV_BUTTON)
                    .register();

    public static final BlockEntityEntry<HvSwitchBlockEntity> HV_SWITCH =
            REGISTRATE.blockEntity("hv_switch", HvSwitchBlockEntity::new)
                    .visual(() -> HvSwitchVisual::new)
                    .validBlock(ModdedBlocks.HV_SWITCH)
                    .renderer(() -> HvSwitchRenderer::new)
                    .register();

    public static final BlockEntityEntry<SparkGapBlockEntity> SPARK_GAP =
            REGISTRATE.blockEntity("spark_gap", SparkGapBlockEntity::new)
                    .validBlock(ModdedBlocks.SPARK_GAP)
                    .renderer(() -> SparkGapRenderer::new)
                    .register();

    public static final BlockEntityEntry<ContactorBlockEntity> CONTACTOR =
            REGISTRATE.blockEntity("contactor", ContactorBlockEntity::new)
                    .validBlock(ModdedBlocks.CONTACTOR)
                    .register();

    public static final BlockEntityEntry<CreativeSourceBlockEntity> CREATIVE_SOURCE =
            REGISTRATE.blockEntity("creative_source", CreativeSourceBlockEntity::new)
                    .validBlocks(ModdedBlocks.CREATIVE_VOLTAGE_SOURCE, ModdedBlocks.CREATIVE_CURRENT_SOURCE)
                    .register();
    public static final BlockEntityEntry<CreativeResistorBlockEntity> CREATIVE_RESISTOR =
            REGISTRATE.blockEntity("creative_resistor", CreativeResistorBlockEntity::new)
                    .validBlock(ModdedBlocks.CREATIVE_RESISTOR)
                    .register();

    public static final BlockEntityEntry<ResistorBlockEntity> RESISTOR =
            REGISTRATE.blockEntity("resistor", ResistorBlockEntity::new)
                    .validBlock(ModdedBlocks.RESISTOR)
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

    public static final BlockEntityEntry<VariacBlockEntity> VARIAC =
            REGISTRATE.blockEntity("variac", VariacBlockEntity::new)
                    .visual(() -> TunedBlockVisual::new)
                    .validBlock(ModdedBlocks.VARIAC)
                    .renderer(() -> TunedBlockRenderer::new)
                    .register();

    public static final BlockEntityEntry<ElectricMotorBlockEntity> ELECTRIC_MOTOR =
            REGISTRATE.blockEntity("electric_motor", ElectricMotorBlockEntity::new)
                    .visual(() -> HalfShaftVisual::new)
                    .validBlock(ModdedBlocks.ELECTRIC_MOTOR)
                    .renderer(() -> ElectricMotorRenderer::new)
                    .register();

    public static final BlockEntityEntry<ConstantSpeedMotorBlockEntity> CONSTANT_SPEED_MOTOR =
            REGISTRATE.blockEntity("constant_speed_motor", ConstantSpeedMotorBlockEntity::new)
                    .visual(() -> HalfShaftVisual::new)
                    .validBlock(ModdedBlocks.CONSTANT_SPEED_MOTOR)
                    .renderer(() -> ElectricMotorRenderer::new)
                    .register();

    public static final BlockEntityEntry<ServoBlockEntity> SERVO =
            REGISTRATE.blockEntity("servo", ServoBlockEntity::new)
                    .visual(() -> HalfShaftVisual::new)
                    .validBlock(ModdedBlocks.SERVO)
                    .renderer(() -> ServoRenderer::new)
                    .register();

    public static final BlockEntityEntry<ElectromagnetBlockEntity> ELECTROMAGNET =
            REGISTRATE.blockEntity("electromagnet", ElectromagnetBlockEntity::new)
                    .validBlock(ModdedBlocks.ELECTROMAGNET)
                    .register();

    public static final BlockEntityEntry<ElectricFanBlockEntity> ELECTRIC_FAN =
            REGISTRATE.blockEntity("electric_fan", ElectricFanBlockEntity::new)
                    .validBlock(ModdedBlocks.ELECTRIC_FAN)
                    .renderer(() -> ElectricFanRenderer::new)
                    .register();

    public static final BlockEntityEntry<PortableBatteryBlockEntity> PORTABLE_BATTERY =
            REGISTRATE.blockEntity("portable_battery", PortableBatteryBlockEntity::new)
                    .validBlock(ModdedBlocks.PORTABLE_BATTERY)
                    .register();

    public static final BlockEntityEntry<WindingBlockEntity> WINDING =
            REGISTRATE.blockEntity("winding", WindingBlockEntity::new)
                    .validBlock(ModdedBlocks.WINDING)
                    .register();

    public static final BlockEntityEntry<CircuitDesignTableBlockEntity> CIRCUIT_DESIGN_TABLE =
            REGISTRATE.blockEntity("circuit_design_table", CircuitDesignTableBlockEntity::new)
                    .validBlock(ModdedBlocks.CIRCUIT_DESIGN_TABLE)
                    .register();

    public static final BlockEntityEntry<CircuitBoardBlockEntity> CIRCUIT_BOARD =
            REGISTRATE.blockEntity("circuit_board", CircuitBoardBlockEntity::new)
                    .validBlock(ModdedBlocks.CIRCUIT_BOARD)
                    .renderer(() -> CircuitBoardRenderer::new)
                    .register();

    public static final BlockEntityEntry<DeviceConnectorBlockEntity> DEVICE_CONNECTOR =
            REGISTRATE.blockEntity("device_connector", SubstituteBlockEntityProvider.INSTANCE.get(DeviceConnectorBlockEntity.class))
                    .validBlock(ModdedBlocks.DEVICE_CONNECTOR)
                    .register();

    public static final BlockEntityEntry<FuseHolderBlockEntity> FUSE_HOLDER =
            REGISTRATE.blockEntity("fuse_holder", FuseHolderBlockEntity::new)
                    .validBlock(ModdedBlocks.FUSE_HOLDER)
                    .register();

    public static final BlockEntityEntry<AlarmBellBlockEntity> ALARM_BELL =
            REGISTRATE.blockEntity("alarm_bell", AlarmBellBlockEntity::new)
                    .validBlock(ModdedBlocks.ALARM_BELL)
                    .register();

    public static final BlockEntityEntry<ThermometerBlockEntity> THERMOMETER =
            REGISTRATE.blockEntity("thermometer", ThermometerBlockEntity::new)
                    .validBlock(ModdedBlocks.THERMOMETER)
                    .renderer(() -> ThermometerRenderer::new)
                    .register();

    public static final BlockEntityEntry<RheostatBlockEntity> RHEOSTAT =
            REGISTRATE.blockEntity("rheostat", RheostatBlockEntity::new)
                    .visual(() -> TunedBlockVisual::new)
                    .validBlock(ModdedBlocks.RHEOSTAT)
                    .renderer(() -> TunedBlockRenderer::new)
                    .register();

    public static final BlockEntityEntry<GroundingRodBlockEntity> GROUNDING_ROD =
            REGISTRATE.blockEntity("grounding_rod", GroundingRodBlockEntity::new)
                    .validBlock(ModdedBlocks.GROUNDING_ROD)
                    .register();

    public static final BlockEntityEntry<SocketBlockEntity> SOCKET =
            REGISTRATE.blockEntity("socket", SocketBlockEntity::new)
                    .validBlock(ModdedBlocks.SOCKET)
                    .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
