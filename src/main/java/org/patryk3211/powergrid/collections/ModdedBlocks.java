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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableBlock;
import org.patryk3211.powergrid.config.CResistance;
import org.patryk3211.powergrid.config.CStress;
import org.patryk3211.powergrid.config.CThermal;
import org.patryk3211.powergrid.electricity.basinheater.BasinHeaterBlock;
import org.patryk3211.powergrid.electricity.basinheater.BasinHeaterBlockEntity;
import org.patryk3211.powergrid.electricity.battery.BatteryBlock;
import org.patryk3211.powergrid.electricity.battery.BatteryCTBehaviour;
import org.patryk3211.powergrid.electricity.battery.PotatoBatteryBlock;
import org.patryk3211.powergrid.electricity.battery.SimpleBatterySpec;
import org.patryk3211.powergrid.electricity.bell.AlarmBellBlock;
import org.patryk3211.powergrid.electricity.carbonpile.CarbonPileBlock;
import org.patryk3211.powergrid.electricity.carbonpile.CarbonPileCoilBlock;
import org.patryk3211.powergrid.electricity.contactor.ContactorBlock;
import org.patryk3211.powergrid.electricity.creative.CreativeResistorBlock;
import org.patryk3211.powergrid.electricity.creative.CreativeSourceBlock;
import org.patryk3211.powergrid.electricity.crt.CRTBlock;
import org.patryk3211.powergrid.electricity.crt.EncasedCRTBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.DeviceConnectorBlock;
import org.patryk3211.powergrid.electricity.electricswitch.*;
import org.patryk3211.powergrid.electricity.electromagnet.ElectromagnetBlock;
import org.patryk3211.powergrid.electricity.fan.ElectricFanBlock;
import org.patryk3211.powergrid.electricity.febridge.FEInverterBlock;
import org.patryk3211.powergrid.electricity.fuse.FuseHolderBlock;
import org.patryk3211.powergrid.electricity.gauge.CurrentGaugeBlock;
import org.patryk3211.powergrid.electricity.gauge.PowerGaugeBlock;
import org.patryk3211.powergrid.electricity.gauge.VoltageGaugeBlock;
import org.patryk3211.powergrid.electricity.grounding.GroundingRodBlock;
import org.patryk3211.powergrid.electricity.heater.HeaterBlock;
import org.patryk3211.powergrid.electricity.light.factorylight.FactoryLightBlock;
import org.patryk3211.powergrid.electricity.light.factorylight.FactoryLightLightBlock;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.electricity.light.string.StringLightBlock;
import org.patryk3211.powergrid.electricity.pump.ElectricPumpBlock;
import org.patryk3211.powergrid.electricity.redstoneconverter.RedstoneConverterBlock;
import org.patryk3211.powergrid.electricity.resistor.ResistorBlock;
import org.patryk3211.powergrid.electricity.socket.SocketBlock;
import org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBearingBlock;
import org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBlock;
import org.patryk3211.powergrid.electricity.solarpanel.SolarPanelCTBehaviour;
import org.patryk3211.powergrid.electricity.sparkgap.SparkGapBlock;
import org.patryk3211.powergrid.electricity.transformer.NetherTransformerBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerCoreBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerSmallBlock;
import org.patryk3211.powergrid.electricity.wireconnector.ConnectorBlock;
import org.patryk3211.powergrid.electricity.wireconnector.CordJunctionBlock;
import org.patryk3211.powergrid.electricity.wireconnector.HeavyConnectorBlock;
import org.patryk3211.powergrid.equipment.portablebattery.PortableBatteryBlock;
import org.patryk3211.powergrid.equipment.thermometer.ThermometerBlock;
import org.patryk3211.powergrid.equipment.thermometer.ThermometerItem;
import org.patryk3211.powergrid.equipment.thermometer.ThermometerItemRenderer;
import org.patryk3211.powergrid.general.ceilingtile.CeilingTileBlock;
import org.patryk3211.powergrid.general.ceilingtile.junction.CeilingTileJunctionBlock;
import org.patryk3211.powergrid.general.ceilingtile.lamp.CeilingTileLampBlock;
import org.patryk3211.powergrid.general.ceilingtile.solar.CeilingTileSolarBlock;
import org.patryk3211.powergrid.general.ceilingtile.solar.CeilingTileSolarBlockCTBehaviour;
import org.patryk3211.powergrid.general.ceilingtile.wire.CeilingTileConnectorBlock;
import org.patryk3211.powergrid.kinetics.generator.clutch.GeneratorClutchBlock;
import org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing;
import org.patryk3211.powergrid.kinetics.generator.housing.VerticalGeneratorHousing;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.CommutatorBlock;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.InductionRotorBlock;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.LargeInductionRotorBlock;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.VerticalCommutatorBlock;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock;
import org.patryk3211.powergrid.kinetics.motor.ConstantSpeedMotorBlock;
import org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlock;
import org.patryk3211.powergrid.kinetics.plotter.PlotterBlock;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardReaderBlock;
import org.patryk3211.powergrid.kinetics.rheostat.RheostatBlock;
import org.patryk3211.powergrid.kinetics.servo.ServoBlock;
import org.patryk3211.powergrid.kinetics.variac.VariacBlock;

import static com.simibubi.create.foundation.data.CreateRegistrate.casingConnectivity;
import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;
import static com.simibubi.create.foundation.data.TagGen.*;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;
import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;
import static org.patryk3211.powergrid.utility.DataProviderUtility.*;

public class ModdedBlocks {
    public static final BlockEntry<BatteryBlock> BATTERY = REGISTRATE.block("battery", BatteryBlock::new)
            .blockstate(cubeColumn("block/battery/battery_side", "block/battery/battery_top"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .transform(BatteryBlock.setSpec(SimpleBatterySpec.ACID_BATTERY))
            .transform(CThermal.maxPower(100, 1.5f))
            .transform(DisplaySource.displaySource(ModdedDisplaySources.BATTERY))
            .onRegister(CreateRegistrate.connectedTextures(BatteryCTBehaviour::new))
            .simpleItem()
            .register();

    public static final BlockEntry<PotatoBatteryBlock> POTATO_BATTERY = REGISTRATE.block("potato_battery", PotatoBatteryBlock::new)
            .blockstate(horizontalBlock(state -> state.getValue(PotatoBatteryBlock.BAKED) ? "block/baked_potato_battery" : "block/potato_battery"))
            .initialProperties(() -> Blocks.NETHER_WART)
            .loot((tables, block) ->
                    tables.add(block, b -> LootTable.lootTable()
                            .withPool(LootPool.lootPool()
                                    .when(ExplosionCondition.survivesExplosion())
                                    .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(b)
                                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(PotatoBatteryBlock.BAKED, false)))
                                    .add(LootItem.lootTableItem(b))
                                    .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                            .copy("Energy", "Energy", CopyNbtFunction.MergeStrategy.REPLACE)))
                            .withPool(LootPool.lootPool()
                                    .when(ExplosionCondition.survivesExplosion())
                                    .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(b)
                                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(PotatoBatteryBlock.BAKED, true)))
                                    .add(LootItem.lootTableItem(Items.BAKED_POTATO)))
                    ))
            .item()
                .model(generated())
                .build()
            .register();

    public static final BlockEntry<CasingBlock> CONDUCTIVE_CASING = REGISTRATE.block("conductive_casing", CasingBlock::new)
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry()))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .onRegister(connectedTextures(() -> new EncasedCTBehaviour(ModdedPartialModels.CONDUCTIVE_CASING)))
            .onRegister(casingConnectivity((block, cc) -> cc.makeCasing(block, ModdedPartialModels.CONDUCTIVE_CASING)))
            .simpleItem()
            .register();
    public static final BlockEntry<CasingBlock> COPPER_PLATING = REGISTRATE.block("copper_plating", CasingBlock::new)
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry()))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .onRegister(connectedTextures(() -> new EncasedCTBehaviour(ModdedPartialModels.COPPER_PLATING)))
            .onRegister(casingConnectivity((block, cc) -> cc.makeCasing(block, ModdedPartialModels.COPPER_PLATING)))
            .simpleItem()
            .register();

    public static final BlockEntry<ConnectorBlock> WIRE_CONNECTOR = REGISTRATE.block("wire_connector", ConnectorBlock::new)
            .blockstate(alternateDirectionalBlock("block/wire_connector"))
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();

    public static final BlockEntry<HeavyConnectorBlock> HEAVY_WIRE_CONNECTOR = REGISTRATE.block("heavy_wire_connector", HeavyConnectorBlock::new)
            .blockstate(alternateDirectionalBlock("block/heavy_wire_connector"))
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();

    public static final BlockEntry<CordJunctionBlock> CORD_JUNCTION = REGISTRATE.block("cord_junction", CordJunctionBlock::new)
            .blockstate(downFacing("block/cord_junction"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .simpleItem()
            .register();

    public static final BlockEntry<HeaterBlock> HEATING_COIL = REGISTRATE.block("heating_coil", HeaterBlock::new)
            .blockstate(horizontalBlock("block/heating_coil"))
            .initialProperties(SharedProperties::softMetal)
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(25))
            .transform(CThermal.maxPower(60, 1.0f))
            .defaultLoot()
            .simpleItem()
            .register();

    public static final BlockEntry<BasinHeaterBlock> BASIN_HEATER = REGISTRATE.block("basin_heater", BasinHeaterBlock::new)
            .blockstate(basinHeater("block/basin_heater"))
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.lightLevel(state -> {
                BlazeBurnerBlock.HeatLevel heat = state.getValue(BasinHeaterBlock.HEAT_LEVEL);

                if (heat == BlazeBurnerBlock.HeatLevel.NONE)
                    return 0;
                if (heat == BlazeBurnerBlock.HeatLevel.KINDLED)
                    return 12;
                if (heat == BlazeBurnerBlock.HeatLevel.SEETHING)
                    return 15;

                return 0;
            }))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(10))
            .onRegister(block -> {
                BoilerHeater.REGISTRY.register(block, BasinHeaterBlockEntity::boilerHeater);
            })
            .simpleItem()
            .register();

    public static final BlockEntry<VoltageGaugeBlock> VOLTAGE_METER = REGISTRATE.block("voltage_gauge", VoltageGaugeBlock::new)
            .blockstate(horizontalBlock("block/gauge/conductive/base"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistances("range_2kv", 2e7, "range_200v", 2e6, "range_20v", 2e5, "range_2v", 2e4))
            .transform(DisplaySource.displaySource(ModdedDisplaySources.ELECTRIC_GAUGE))
            .item()
                .model(gauge("block/gauge/item_voltage", "block/conductive_gauge"))
                .build()
            .register();

    public static final BlockEntry<CurrentGaugeBlock> CURRENT_METER = REGISTRATE.block("current_gauge", CurrentGaugeBlock::new)
            .blockstate(horizontalBlock("block/gauge/conductive/base"))
            .initialProperties(SharedProperties::softMetal)
            .transform(CResistance.setResistance(0.05f))
            .transform(CThermal.maxPower(35, 2.0f))
            .transform(pickaxeOnly())
            .transform(DisplaySource.displaySource(ModdedDisplaySources.ELECTRIC_GAUGE))
            .item()
                .model(gauge("block/gauge/item_current", "block/conductive_gauge"))
                .build()
            .register();

    public static final BlockEntry<PowerGaugeBlock> POWER_METER = REGISTRATE.block("power_gauge", PowerGaugeBlock::new)
            .blockstate(horizontalBlock("block/gauge/conductive/base_power"))
            .initialProperties(SharedProperties::softMetal)
            .transform(CResistance.setResistances("series", 0.05f, "shunt_range_20kw", 2e7, "shunt_range_2kw", 2e7, "shunt_range_200w", 2e6, "shunt_range_20w", 2e5))
            .transform(CThermal.maxPower(35, 2.0f))
            .transform(pickaxeOnly())
            .transform(DisplaySource.displaySource(ModdedDisplaySources.ELECTRIC_GAUGE))
            .item()
                .model(gauge("block/gauge/item_power", "block/conductive_gauge"))
                .build()
            .register();

    public static final BlockEntry<PlotterBlock> PLOTTER = REGISTRATE.block("plotter", PlotterBlock::new)
            .blockstate(horizontalBlock("block/plotter/base"))
            .initialProperties(SharedProperties::wooden)
            .addLayer(() -> RenderType::translucent)
            .transform(CStress.setImpact(2.0))
            .transform(axeOrPickaxe())
            .item()
                .model(itemWithParent("block/plotter/item"))
                .build()
            .register();

//    public static final BlockEntry<RotorBlock> GENERATOR_ROTOR = REGISTRATE.block("generator_rotor", RotorBlock::new)
//            .blockstate(rotorModel("block/generator/rotor"))
//            .initialProperties(SharedProperties::stone)
//            .properties(BlockBehaviour.Properties::noOcclusion)
//            .transform(pickaxeOnly())
//            .transform(CStress.setImpact(32))
//            .defaultLoot()
//            .item()
//                .model(itemWithParent("block/generator/rotor"))
//                .build()
//            .lang("Generator Rotor")
//            .register();

    public static final BlockEntry<InductionRotorBlock> GENERATOR_INDUCTION_ROTOR = REGISTRATE.block("generator_induction_rotor", InductionRotorBlock::new)
            .blockstate(rotorModel("block/generator/induction_rotor"))
            .initialProperties(SharedProperties::softMetal)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(0.5))
            .transform(CStress.setImpact(32))
            .defaultLoot()
            .item()
                .model(itemWithParent("block/generator/induction_rotor"))
                .build()
            .register();

    public static final BlockEntry<LargeInductionRotorBlock> GENERATOR_LARGE_INDUCTION_ROTOR = REGISTRATE.block("generator_large_induction_rotor", LargeInductionRotorBlock::new)
            .blockstate(rotorModel("block/generator/large_induction_rotor"))
            .initialProperties(SharedProperties::softMetal)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(1.0))
            .transform(CStress.setImpact(128))
            .defaultLoot()
            .item()
                .model(itemWithParent("block/generator/large_induction_rotor"))
                .build()
            .register();

    public static final BlockEntry<CommutatorBlock> GENERATOR_COMMUTATOR = REGISTRATE.block("generator_commutator", CommutatorBlock::new)
            .blockstate(horizontalBlock("block/generator/commutator_base_horizontal"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .transform(CStress.setImpact(8))
            .defaultLoot()
            .item()
                .model(itemWithParent("block/generator/commutator"))
                .build()
            .register();

    public static final BlockEntry<VerticalCommutatorBlock> GENERATOR_VERTICAL_COMMUTATOR = REGISTRATE.block("generator_vertical_commutator", VerticalCommutatorBlock::new)
            .blockstate(verticalCommutator("block/generator/commutator_base_vertical"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .transform(CStress.setImpact(8))
            .defaultLoot()
            .lang("Vertical Generator Commutator")
            .item()
            .model(itemWithParent("block/generator/vertical_commutator"))
            .build()
            .register();

    public static final BlockEntry<GeneratorClutchBlock> GENERATOR_CLUTCH = REGISTRATE.block("generator_clutch", GeneratorClutchBlock::new)
            .blockstate(alternateDirectionalBlock(state -> state.getValue(POWERED) ? "block/generator/clutch_on" : "block/generator/clutch"))
            .initialProperties(SharedProperties::wooden)
            .transform(axeOrPickaxe())
            .transform(CStress.setImpact(32))
            .transform(DisplaySource.displaySource(ModdedDisplaySources.CLUTCH))
            .tag(ModdedTags.Block.IGNORE_IN_ROTOR_ASSEMBLY_SIZE.tag)
            .defaultLoot()
            .item()
                .model(itemWithParent("block/generator/clutch_item"))
                .build()
            .register();

    public static final BlockEntry<GeneratorHousing> GENERATOR_HOUSING = REGISTRATE.block("generator_housing", GeneratorHousing::new)
            .blockstate(housing("block/generator/housing"))
            .initialProperties(SharedProperties::softMetal)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(pickaxeOnly())
            .addLayer(() -> RenderType::cutoutMipped)
            .defaultLoot()
            .item()
                .model(itemWithParent("block/generator/housing"))
                .build()
            .register();

    public static final BlockEntry<VerticalGeneratorHousing> VERTICAL_GENERATOR_HOUSING = REGISTRATE.block("vertical_generator_housing", VerticalGeneratorHousing::new)
            .blockstate(horizontalBlock("block/generator/housing_vertical"))
            .initialProperties(SharedProperties::softMetal)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(pickaxeOnly())
            .addLayer(() -> RenderType::cutoutMipped)
            .defaultLoot()
            .item()
                .model(itemWithParent("block/generator/housing_vertical"))
                .build()
            .register();

    public static final BlockEntry<LvSwitchBlock> LV_SWITCH = REGISTRATE.block("lv_switch", LvSwitchBlock::new)
            .blockstate(surfaceSwitch("block/switches/lv_switch"))
            .initialProperties(SharedProperties::wooden)
            .transform(axeOrPickaxe())
            .transform(CResistance.setResistance(0.15))
            .transform(CThermal.maxPower(38.4, 0.5f))
            .lang("LV Switch")
            .item()
                .model(itemWithParent("block/switches/lv_switch_off_v"))
                .build()
            .register();

    public static final BlockEntry<LvButtonBlock> LV_BUTTON = REGISTRATE.block("lv_button", LvButtonBlock::new)
            .blockstate(surfaceSwitch("block/switches/lv_button"))
            .initialProperties(SharedProperties::wooden)
            .transform(axeOrPickaxe())
            .transform(CResistance.setResistance(0.15))
            .transform(CThermal.maxPower(38.4, 0.5f))
            .lang("LV Button")
            .item()
                .model(itemWithParent("block/switches/lv_button_off_v"))
                .build()
            .register();

    public static final BlockEntry<MvSwitchBlock> MV_SWITCH = REGISTRATE.block("mv_switch", MvSwitchBlock::new)
            .blockstate(surfaceSwitch("block/switches/mv_switch"))
            .lang("MV Switch")
            .initialProperties(SharedProperties::wooden)
            .transform(axeOrPickaxe())
            .transform(CResistance.setResistance(0.05))
            .transform(CThermal.maxPower(51.2, 1.0f))
            .item()
                .model(itemWithParent("block/switches/mv_switch_off_v"))
                .build()
            .register();

    public static final BlockEntry<HvSwitchBlock> HV_SWITCH = REGISTRATE.block("hv_switch", HvSwitchBlock::new)
            .blockstate(hvSwitch("block/switches/hv_switch"))
            .initialProperties(SharedProperties::stone)
            .transform(axeOrPickaxe())
            .transform(CStress.setImpact(2))
            .transform(CResistance.setResistance(0.1))
            .transform(CThermal.maxPower(153.6, 2.0f))
            .loot((tables, block) ->
                    tables.add(block, b -> LootTable.lootTable()
                            .withPool(LootPool.lootPool()
                                    .when(ExplosionCondition.survivesExplosion())
                                    .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(b)
                                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(HvSwitchBlock.PART, 0)))
                                    .add(LootItem.lootTableItem(block)))
                    ))
            .lang("HV Switch")
            .item()
                .model(itemWithParent("block/switches/hv_switch"))
                .build()
            .register();

    public static final BlockEntry<HvBreakerBlock> HV_BREAKER = REGISTRATE.block("hv_breaker", HvBreakerBlock::new)
            .blockstate(horizontalBlock("block/switches/hv_breaker_block"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .transform(CStress.setImpact(2))
            .transform(CResistance.setResistance(0.1))
            .transform(CThermal.maxPower(102.4, 2.0f))
            .lang("HV Breaker")
            .item()
                .model(itemWithParent("block/switches/hv_breaker_item"))
                .build()
            .register();

    public static final BlockEntry<SparkGapBlock> SPARK_GAP = REGISTRATE.block("spark_gap", SparkGapBlock::new)
            .blockstate(horizontalAxisBlock("block/spark_gap/block"))
            .initialProperties(SharedProperties::wooden)
            .transform(axeOrPickaxe())
            .item()
                .model(itemWithParent("block/spark_gap/item"))
                .build()
            .register();

    public static final BlockEntry<ContactorBlock> CONTACTOR = REGISTRATE.block("contactor", ContactorBlock::new)
            .blockstate(horizontalAxisBlock("block/contactor"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistances("coil", 12, "switch", 0.05))
            .transform(CThermal.maxPower(252.8, 2.0f))
            .simpleItem()
            .register();

    public static final BlockEntry<CreativeSourceBlock> CREATIVE_VOLTAGE_SOURCE = REGISTRATE.block("creative_voltage_source", CreativeSourceBlock::new)
            .blockstate(horizontalBlock("block/creative_voltage_source"))
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();
    public static final BlockEntry<CreativeSourceBlock> CREATIVE_CURRENT_SOURCE = REGISTRATE.block("creative_current_source", CreativeSourceBlock::new)
            .blockstate(horizontalBlock("block/creative_current_source"))
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();
    public static final BlockEntry<CreativeResistorBlock> CREATIVE_RESISTOR = REGISTRATE.block("creative_resistor", CreativeResistorBlock::new)
            .blockstate(surfaceBlock("block/creative_resistor"))
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .defaultLoot()
            .item()
                .model(itemWithParent("block/creative_resistor_v"))
                .build()
            .register();

    public static final BlockEntry<ResistorBlock> RESISTOR = REGISTRATE.block("power_resistor", ResistorBlock::new)
            .blockstate(surfaceBlock("block/resistor"))
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .transform(CThermal.maxPower(1000, 2.0f))
            .item()
                .model(itemWithParent("block/resistor_v"))
                .build()
            .register();

    public static final BlockEntry<LightFixtureBlock> LIGHT_FIXTURE = REGISTRATE.block("light_fixture", LightFixtureBlock::new)
            .blockstate(lightFixture("block/fixtures/light_fixture"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .transform(LightFixtureBlock.setBulbModelOffset(0, 3 / 16f, 0))
            .defaultLoot()
            .item()
                .model(itemWithParent("block/fixtures/light_fixture_v"))
                .build()
            .register();

    public static final BlockEntry<FactoryLightBlock> FACTORY_LIGHT = REGISTRATE.block("factory_light", FactoryLightBlock::new)
            .blockstate(factoryLight("block/factory_light"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .item()
                .model(itemWithParent("block/factory_light/factorylight"))
                .build()
            .register();

    public static final BlockEntry<FactoryLightLightBlock> FACTORY_LIGHT_LIGHT = REGISTRATE.block("factory_light_light", FactoryLightLightBlock::new)
            .blockstate(air())
            .register();

    public static final BlockEntry<CeilingTileBlock> CEILING_TILE = REGISTRATE.block("ceiling_tile", CeilingTileBlock::new)
            .blockstate(simple("block/ceiling_tile/ceiling_tile"))
            .initialProperties(SharedProperties::stone)
            .transform(axeOrPickaxe())
            .item()
                .model(itemWithParent("block/ceiling_tile/ceiling_tile"))
                .build()
            .register();

    public static final BlockEntry<CeilingTileLampBlock> CEILING_TILE_LAMP = REGISTRATE.block("ceiling_tile_lamp", CeilingTileLampBlock::new)
            .blockstate(ceilingTileLamp("block/ceiling_tile"))
            .initialProperties(SharedProperties::stone)
            .transform(axeOrPickaxe())
            .addLayer(() -> RenderType::cutout)
            .loot((tables, block) -> {
                tables.dropOther(block, FACTORY_LIGHT);
                tables.dropOther(block, CEILING_TILE);
            })
            .register();

    public static final BlockEntry<CeilingTileConnectorBlock> CEILING_TILE_CONNECTOR = REGISTRATE.block("ceiling_tile_connector", CeilingTileConnectorBlock::new)
            .blockstate(simple("block/ceiling_tile/ceiling_tile_connector"))
            .initialProperties(SharedProperties::stone)
            .transform(axeOrPickaxe())
            .loot((tables, block) -> {
                tables.dropOther(block, WIRE_CONNECTOR);
                tables.dropOther(block, CEILING_TILE);
            })
            .register();

    public static final BlockEntry<CeilingTileJunctionBlock> CEILING_TILE_JUNCTION = REGISTRATE.block("ceiling_tile_junction", CeilingTileJunctionBlock::new)
            .blockstate(simple("block/ceiling_tile/ceiling_tile_cord_junction"))
            .initialProperties(SharedProperties::stone)
            .transform(axeOrPickaxe())
            .loot((tables, block) -> {
                tables.dropOther(block, CORD_JUNCTION);
                tables.dropOther(block, CEILING_TILE);
            })
            .register();

    public static final BlockEntry<TransformerCoreBlock> TRANSFORMER_CORE = REGISTRATE.block("transformer_core", TransformerCoreBlock::new)
            .blockstate(cubeAllWithItem("block/transformer/core"))
            .initialProperties(SharedProperties::softMetal)
            .properties(properties -> properties.sound(SoundType.NETHERITE_BLOCK))
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();
    public static final BlockEntry<TransformerSmallBlock> TRANSFORMER_SMALL = REGISTRATE.block("transformer_small", TransformerSmallBlock::new)
            .initialProperties(TRANSFORMER_CORE)
            .blockstate(transformerSmall())
            .loot((tables, block) -> tables.dropOther(block, TRANSFORMER_CORE.get()))
            .properties(properties -> properties.sound(SoundType.NETHERITE_BLOCK))
            .transform(pickaxeOnly())
            .transform(CThermal.maxPower(1000, 4.0f))
            .register();
    public static final BlockEntry<TransformerMediumBlock> TRANSFORMER_MEDIUM = REGISTRATE.block("transformer_medium", TransformerMediumBlock::new)
            .initialProperties(TRANSFORMER_CORE)
            .blockstate(transformerMedium())
            .loot((tables, block) -> tables.dropOther(block, TRANSFORMER_CORE.get()))
            .properties(properties -> properties.sound(SoundType.NETHERITE_BLOCK))
            .transform(pickaxeOnly())
            .transform(CThermal.maxPower(4000, 16.0f))
            .register();
    public static final BlockEntry<NetherTransformerBlock> NETHER_TRANSFORMER = REGISTRATE.block("nether_transformer", NetherTransformerBlock::new)
            .initialProperties(TRANSFORMER_CORE)
            .blockstate(transformerNether())
            .loot((tables, block) -> tables.dropOther(block, TRANSFORMER_CORE.get()))
            .transform(pickaxeOnly())
            .transform(CThermal.maxPower(4000, 16.0f))
            .register();

    public static final BlockEntry<VariacBlock> VARIAC = REGISTRATE.block("variac", VariacBlock::new)
            .initialProperties(TRANSFORMER_CORE)
            .blockstate(tunedBlock("block/variac/block", "block/variac/block_baseless"))
            .transform(pickaxeOnly())
            .transform(CStress.setNoImpact())
            .transform(CThermal.maxPower(1000, 4.0f))
            .item()
                .model(itemWithParent("block/variac/item"))
                .build()
            .register();

    public static final BlockEntry<ElectricMotorBlock> ELECTRIC_MOTOR = REGISTRATE.block("electric_motor", ElectricMotorBlock::new)
            .blockstate(alternateDirectionalBlock(state -> switch(state.getValue(ElectricMotorBlock.FACING).getAxis()) {
                        case X, Z -> "block/electric_motor/block";
                        case Y -> "block/electric_motor/block_vertical";
                    }))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .transform(CStress.setCapacity(64))
            .transform(CResistance.setResistance(25.6))
            .transform(pickaxeOnly())
            .onRegister(BlockStressValues.setGeneratorSpeed(256, true))
            .defaultLoot()
            .item()
                .model(itemWithParent("block/electric_motor/item"))
                .build()
            .register();

    public static final BlockEntry<ConstantSpeedMotorBlock> CONSTANT_SPEED_MOTOR = REGISTRATE.block("constant_speed_motor", ConstantSpeedMotorBlock::new)
            .blockstate(alternateDirectionalBlock(state -> switch(state.getValue(ConstantSpeedMotorBlock.FACING).getAxis()) {
                case X, Z -> "block/constant_electric_motor/block";
                case Y -> "block/constant_electric_motor/block_vertical";
            }))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .transform(CStress.setCapacity(64))
            .transform(CResistance.setResistance(25.6))
            .transform(pickaxeOnly())
            .onRegister(BlockStressValues.setGeneratorSpeed(256, true))
            .defaultLoot()
            .item()
            .model(itemWithParent("block/constant_electric_motor/item"))
            .build()
            .register();

    public static final BlockEntry<ServoBlock> SERVO = REGISTRATE.block("servo", ServoBlock::new)
            .blockstate(alternateDirectionalBlock(state -> switch(state.getValue(ElectricMotorBlock.FACING).getAxis()) {
                case X, Z -> "block/servo/block";
                case Y -> "block/servo/block_vertical";
            }))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .transform(CStress.setCapacity(32))
            .transform(CResistance.setResistances("on", 12.8, "idle", 64))
            .transform(pickaxeOnly())
            .onRegister(BlockStressValues.setGeneratorSpeed(32, true))
            .defaultLoot()
            .item()
                .model(itemWithParent("block/servo/item"))
                .build()
            .register();

    public static final BlockEntry<ElectromagnetBlock> ELECTROMAGNET = REGISTRATE.block("electromagnet", ElectromagnetBlock::new)
            .blockstate(simple("block/electromagnet"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(15))
            .transform(CThermal.maxPower(1500, 4.0f))
            .defaultLoot()
            .item(AssemblyOperatorBlockItem::new)
            .build()
            .register();

    public static final BlockEntry<ElectricFanBlock> ELECTRIC_FAN = REGISTRATE.block("electric_fan", ElectricFanBlock::new)
            .blockstate(upFacing("block/electric_fan/block"))
            .initialProperties(SharedProperties::stone)
            .transform(axeOrPickaxe())
            .transform(CResistance.setResistance(20))
            .addLayer(() -> RenderType::cutoutMipped)
            .defaultLoot()
            .item()
                .model(itemWithParent("block/electric_fan/item"))
                .build()
            .register();

    public static final BlockEntry<ElectricPumpBlock> ELECTRIC_PUMP = REGISTRATE.block("electric_pump", ElectricPumpBlock::new)
            .blockstate(alternateDirectionalBlock("block/electric_pump"))
            .initialProperties(SharedProperties::copperMetal)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(480))
            .simpleItem()
            .register();

    public static final BlockEntry<PortableBatteryBlock> PORTABLE_BATTERY = REGISTRATE.block("portable_battery", PortableBatteryBlock::new)
            .blockstate(horizontalBlock("block/portable_battery/block"))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(25))
            .transform(CThermal.maxPower(100, 1.0f))
            .loot((tables, block) -> tables.add(block, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .when(ExplosionCondition.survivesExplosion())
                            .add(LootItem.lootTableItem(ModdedItems.PORTABLE_BATTERY)))
                    .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                            .copy("Charge", "Charge", CopyNbtFunction.MergeStrategy.REPLACE))
            ))
            .register();

    public static final BlockEntry<CircuitDesignTableBlock> CIRCUIT_DESIGN_TABLE = REGISTRATE.block("circuit_design_table", CircuitDesignTableBlock::new)
            .blockstate(horizontalBlock("block/circuit_design_table"))
            .initialProperties(() -> Blocks.CRAFTING_TABLE)
            .transform(axeOrPickaxe())
            .transform(CResistance.setResistance(480))
            .transform(CThermal.maxPower(50, 0.5f))
            .addLayer(() -> RenderType::cutout)
            .defaultLoot()
            .simpleItem()
            .register();

    public static final BlockEntry<CircuitBoardBlock> CIRCUIT_BOARD = REGISTRATE.block("circuit_board", CircuitBoardBlock::new)
            .blockstate(circuitBoard())
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .loot((tables, block) ->
                    tables.add(block, LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .when(ExplosionCondition.survivesExplosion())
                                .add(LootItem.lootTableItem(ModdedBlocks.CIRCUIT_BOARD)))
                        .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                .copy("Schematic", "Schematic", CopyNbtFunction.MergeStrategy.REPLACE)
                                .copy("Thermal", "Thermal", CopyNbtFunction.MergeStrategy.REPLACE))
            ))
            .item()
                .defaultModel()
                .tag(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)
                .build()
            .register();

    public static final BlockEntry<WindingBlock> WINDING = REGISTRATE.block("winding", WindingBlock::new)
            .blockstate(windingModel())
            .initialProperties(SharedProperties::copperMetal)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(7.5))
            .transform(CThermal.maxPower(200, 1.5f))
            .addLayer(() -> RenderType::cutoutMipped)
            .loot((tables, block) -> tables.dropOther(block, ModdedItems.COPPER_COIL))
            .register();

    public static final BlockEntry<DeviceConnectorBlock> DEVICE_CONNECTOR = REGISTRATE.block("device_connector", DeviceConnectorBlock::new)
            .blockstate(surfaceBlock("block/device_connector"))
            .transform(pickaxeOnly())
            .item()
                .model(itemWithParent("block/device_connector_v"))
                .build()
            .register();

    public static final BlockEntry<SocketBlock> SOCKET = REGISTRATE.block("socket", SocketBlock::new)
            .blockstate(surfaceBlock("block/power_plug"))
            .transform(pickaxeOnly())
            .item()
                .model(itemWithParent("block/power_plug_v"))
                .build()
            .register();

    public static final BlockEntry<RedstoneConverterBlock> REDSTONE_CONVERTER = REGISTRATE.block("redstone_converter", RedstoneConverterBlock::new)
            .blockstate(surfaceBlock(state -> state.getValue(POWERED) ? "block/redstone_converter/on" : "block/redstone_converter/off"))
            .transform(pickaxeOnly())
            .transform(CResistance.setResistances("min", 5, "max", 1000))
            .transform(CThermal.maxPower(50, 0.75f))
            .item()
                .model(itemWithParent("block/redstone_converter/off_v"))
                .build()
            .register();

    public static final BlockEntry<FEInverterBlock> FE_INVERTER = REGISTRATE.block("fe_inverter", FEInverterBlock::new)
            .blockstate(surfaceBlock("block/fe_inverter/block"))
            .transform(axeOrPickaxe())
            .lang("FE Inverter")
            .item()
                .model(itemWithParent("block/fe_inverter/block_v"))
                .build()
            .register();

    public static final BlockEntry<FuseHolderBlock> FUSE_HOLDER = REGISTRATE.block("fuse_holder", FuseHolderBlock::new)
            .blockstate(fuseHolder())
            .initialProperties(SharedProperties::wooden)
            .transform(axeOrPickaxe())
            .transform(CResistance.setResistance(0.2))
            .simpleItem()
            .register();

    public static final BlockEntry<AlarmBellBlock> ALARM_BELL = REGISTRATE.block("alarm_bell", AlarmBellBlock::new)
            .blockstate(horizontalBlock("alarm_bell"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(20))
            .transform(CThermal.maxPower(50, 1.5f))
            .simpleItem()
            .register();

    public static final BlockEntry<ThermometerBlock> THERMOMETER = REGISTRATE.block("thermometer", ThermometerBlock::new)
            .blockstate(northFacing("block/thermometer/base"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .item(ThermometerItem::new)
                .model(itemWithParent("block/thermometer/base"))
                .transform(ModdedItems.customRenderer(() -> ThermometerItemRenderer::new))
                .build()
            .register();

    public static final BlockEntry<RheostatBlock> RHEOSTAT = REGISTRATE.block("rheostat", RheostatBlock::new)
            .initialProperties(CONDUCTIVE_CASING)
            .blockstate(tunedBlock("block/rheostat/block", "block/rheostat/block_baseless"))
            .transform(pickaxeOnly())
            .transform(CStress.setNoImpact())
            .transform(CThermal.maxPower(1000, 4.0f))
            .item()
                .model(itemWithParent("block/rheostat/item"))
                .build()
            .register();

    public static final BlockEntry<GroundingRodBlock> GROUNDING_ROD = REGISTRATE.block("grounding_rod", GroundingRodBlock::new)
            .initialProperties(() -> Blocks.LIGHTNING_ROD)
            .blockstate(simple("block/grounding_rod"))
            .transform(pickaxeOnly())
            .simpleItem()
            .register();

    public static final BlockEntry<CarbonPileCoilBlock> CARBON_PILE_COIL = REGISTRATE.block("carbon_pile_coil", CarbonPileCoilBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .blockstate(horizontalBlock("block/carbon_pile/coil"))
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(50))
            .transform(CThermal.maxPower(100, 2.0f))
            .item()
                .model(itemWithParent("block/carbon_pile/coil"))
                .build()
            .register();

    public static final BlockEntry<CarbonPileBlock> CARBON_PILE = REGISTRATE.block("carbon_pile", CarbonPileBlock::new)
            .initialProperties(() -> Blocks.COAL_BLOCK)
            .blockstate(carbonPile("block/carbon_pile"))
            .transform(pickaxeOnly())
            .transform(CResistance.setResistance(20))
            .transform(CThermal.maxPower(1000, 2.0f))
            .register();

    public static final BlockEntry<CRTBlock> CRT = REGISTRATE.block("crt", CRTBlock::new)
            .initialProperties(() -> Blocks.GLASS)
            .lang("Cathode Ray Tube")
            .blockstate(horizontalBlock("block/crt"))
            .addLayer(() -> RenderType::translucent)
            .transform(pickaxeOnly())
            .transform(CResistance.setResistances("heater", 12, "coils", 40))
            .transform(CThermal.maxPower(100, 3.0f))
            .simpleItem()
            .register();

    public static final BlockEntry<EncasedCRTBlock> ANDESITE_CRT = REGISTRATE.block("andesite_encased_crt", p -> new EncasedCRTBlock(p, AllBlocks.ANDESITE_CASING::get))
            .initialProperties(SharedProperties::wooden)
            .lang("Andesite Encased CRT")
            .blockstate(horizontalBlock("block/crt_andesite"))
            .addLayer(() -> RenderType::translucent)
            .transform(axeOrPickaxe())
            .properties(p -> p.mapColor(MapColor.PODZOL))
            .transform(EncasingRegistry.addVariantTo(CRT))
            .register();

    public static final BlockEntry<EncasedCRTBlock> BRASS_CRT = REGISTRATE.block("brass_encased_crt", p -> new EncasedCRTBlock(p, AllBlocks.BRASS_CASING::get))
            .initialProperties(SharedProperties::wooden)
            .lang("Brass Encased CRT")
            .blockstate(horizontalBlock("block/crt_brass"))
            .addLayer(() -> RenderType::translucent)
            .transform(axeOrPickaxe())
            .properties(p -> p.mapColor(MapColor.PODZOL))
            .transform(EncasingRegistry.addVariantTo(CRT))
            .register();

    public static BlockEntry<PunchCardReaderBlock> PUNCH_CARD_READER = REGISTRATE.block("punch_card_reader", PunchCardReaderBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .blockstate(horizontalBlock("block/punch_card_reader/block"))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(pickaxeOnly())
            .transform(CStress.setImpact(2))
            .transform(CResistance.setResistance(1f))
            .transform(CThermal.maxPower(10, 0.2f))
            .item()
                .model(itemWithParent("block/punch_card_reader/item"))
                .build()
            .register();

    public static BlockEntry<SolarPanelBlock> SOLAR_PANEL = REGISTRATE.block("solar_panel", SolarPanelBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .addLayer(() -> RenderType::translucent)
            .blockstate(alternateDirectionalBlock("block/solar_panel/inline_solar_panel"))
            .onRegister(CreateRegistrate.connectedTextures(SolarPanelCTBehaviour::new))
            .item()
                .model(itemWithParent("block/solar_panel/inline_solar_panel"))
                .build()
            .register();

    public static BlockEntry<SolarPanelBearingBlock> SOLAR_PANEL_BEARING = REGISTRATE.block("solar_panel_bearing", SolarPanelBearingBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate(northFacing("block/solar_panel/solar_panel_bearing_base"))
            .item()
                .model(itemWithParent("block/solar_panel/solar_panel_bearing_block"))
                .build()
            .register();

    public static final BlockEntry<CeilingTileSolarBlock> CEILING_TILE_SOLAR = REGISTRATE.block("ceiling_tile_solar", CeilingTileSolarBlock::new)
            .blockstate(simple("block/ceiling_tile/ceiling_tile_solar_panel"))
            .initialProperties(SharedProperties::stone)
            .addLayer(() -> RenderType::translucent)
            .transform(axeOrPickaxe())
            .onRegister(CreateRegistrate.connectedTextures(CeilingTileSolarBlockCTBehaviour::new))
            .loot((tables, block) -> {
                tables.dropOther(block, SOLAR_PANEL);
                tables.dropOther(block, CEILING_TILE);
            })
            .register();

    public static BlockEntry<StringLightBlock> STRING_LIGHT_BLOCK = REGISTRATE.block("string_light_block", StringLightBlock::new)
            .blockstate(air())
            .register();

    public static void register() {
        BlockMovementChecks.registerAttachedCheck((BlockState state, Level world, BlockPos pos, Direction direction) -> {
            if (state.getBlock() instanceof BatteryBlock && ConnectivityHandler.isConnected(world, pos, pos.relative(direction)))
                return CheckResult.SUCCESS;

            return CheckResult.PASS;
        });
    }
}
