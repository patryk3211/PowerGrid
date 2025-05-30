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
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import io.github.fabricators_of_create.porting_lib.models.generators.ConfiguredModel;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.block.MultiPartBlockStateBuilder;
import io.github.fabricators_of_create.porting_lib.tags.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import org.apache.logging.log4j.util.TriConsumer;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlock;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatCTBehaviour;
import org.patryk3211.powergrid.electricity.battery.BatteryBlock;
import org.patryk3211.powergrid.electricity.creative.CreativeResistorBlock;
import org.patryk3211.powergrid.electricity.creative.CreativeSourceBlock;
import org.patryk3211.powergrid.electricity.electricswitch.LvSwitchBlock;
import org.patryk3211.powergrid.electricity.electricswitch.MvSwitchBlock;
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
import org.patryk3211.powergrid.electricity.wireconnector.HeavyConnectorBlock;
import org.patryk3211.powergrid.kinetics.basicgenerator.BasicGeneratorBlock;
import org.patryk3211.powergrid.kinetics.generator.coil.CoilBlock;
import org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlock;
import org.patryk3211.powergrid.kinetics.generator.rotor.ShaftDirection;
import org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlock;

import java.util.function.Function;

import static com.simibubi.create.foundation.data.TagGen.*;
import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedBlocks {
    public static final BlockEntry<BasicGeneratorBlock> BASIC_GENERATOR = REGISTRATE.block("basic_generator", BasicGeneratorBlock::new)
            .blockstate((ctx, prov) ->
                    prov.horizontalBlock(ctx.getEntry(), modModel(prov, "block/basic_generator")))
            .transform(BlockStressDefaults.setImpact(4.0))
            .simpleItem()
            .register();

    public static final BlockEntry<BatteryBlock> BATTERY = REGISTRATE.block("battery", BatteryBlock::new)
            .blockstate((ctx, prov) ->
                    prov.simpleBlock(ctx.getEntry(), modModel(prov, "block/battery")))
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

    public static final BlockEntry<HeaterBlock> HEATING_COIL = REGISTRATE.block("heating_coil", HeaterBlock::new)
            .blockstate(horizontalBlock("block/heating_coil"))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();

    public static final BlockEntry<VoltageGaugeBlock> ANDESITE_VOLTAGE_METER = REGISTRATE.block("andesite_voltage_gauge", VoltageGaugeBlock::new)
            .blockstate(horizontalBlock("block/gauge/andesite/base"))
            .initialProperties(SharedProperties::wooden)
            .transform(GaugeBlock.setMaxValue(20))
            .transform(GaugeBlock.setMaterial(GaugeBlock.Material.ANDESITE))
            .transform(axeOrPickaxe())
            .defaultLoot()
            .item()
                .model((ctx, prov) ->
                        prov.withExistingParent(ctx.getName(), prov.modLoc("block/gauge/item_voltage"))
                                .texture("2", prov.modLoc("block/andesite_gauge")))
                .build()
            .register();
    public static final BlockEntry<VoltageGaugeBlock> BRASS_VOLTAGE_METER = REGISTRATE.block("brass_voltage_gauge", VoltageGaugeBlock::new)
            .blockstate(horizontalBlock("block/gauge/brass/base"))
            .initialProperties(SharedProperties::wooden)
            .transform(GaugeBlock.setMaxValue(200))
            .transform(GaugeBlock.setMaterial(GaugeBlock.Material.BRASS))
            .transform(axeOrPickaxe())
            .defaultLoot()
            .item()
                .model((ctx, prov) ->
                        prov.withExistingParent(ctx.getName(), prov.modLoc("block/gauge/item_voltage"))
                                .texture("2", prov.modLoc("block/brass_gauge")))
                .build()
            .register();

    public static final BlockEntry<CurrentGaugeBlock> ANDESITE_CURRENT_METER = REGISTRATE.block("andesite_current_gauge", CurrentGaugeBlock::new)
            .blockstate(horizontalBlock("block/gauge/andesite/base"))
            .initialProperties(SharedProperties::wooden)
            .transform(GaugeBlock.setMaxValue(5))
            .transform(GaugeBlock.setMaterial(GaugeBlock.Material.ANDESITE))
            .transform(CurrentGaugeBlock.setResistance(0.25f))
            .transform(axeOrPickaxe())
            .defaultLoot()
            .item()
                .model((ctx, prov) ->
                        prov.withExistingParent(ctx.getName(), prov.modLoc("block/gauge/item_current"))
                                .texture("2", prov.modLoc("block/andesite_gauge")))
                .build()
            .register();
    public static final BlockEntry<CurrentGaugeBlock> BRASS_CURRENT_METER = REGISTRATE.block("brass_current_gauge", CurrentGaugeBlock::new)
            .blockstate(horizontalBlock("block/gauge/brass/base"))
            .initialProperties(SharedProperties::wooden)
            .transform(GaugeBlock.setMaxValue(25))
            .transform(GaugeBlock.setMaterial(GaugeBlock.Material.BRASS))
            .transform(CurrentGaugeBlock.setResistance(0.05f))
            .transform(axeOrPickaxe())
            .defaultLoot()
            .item()
                .model((ctx, prov) ->
                        prov.withExistingParent(ctx.getName(), prov.modLoc("block/gauge/item_current"))
                                .texture("2", prov.modLoc("block/brass_gauge")))
                .build()
            .register();

    public static final BlockEntry<RotorBlock> ROTOR = REGISTRATE.block("rotor", RotorBlock::new)
            .blockstate((ctx, prov) ->
                    prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                        var shaftDir = state.get(RotorBlock.SHAFT_DIRECTION);
                        if(shaftDir == ShaftDirection.NONE)
                            return ConfiguredModel.builder().modelFile(modModel(prov, "block/rotor/rotor_particle")).build();
                        int x = 90;
                        int y = 0;
                        if(shaftDir == ShaftDirection.NEGATIVE) {
                            y = 180;
                            x = -90;
                        }
                        var builder = ConfiguredModel.builder().modelFile(modModel(prov, "block/rotor/rotor_plate"));
                        switch(state.get(RotorBlock.AXIS)) {
                            case X -> builder.rotationY(y - 90);
                            case Z -> builder.rotationY(y);
                            case Y -> builder.rotationX(x);
                        };
                        return builder.build();
                    }))
            .initialProperties(SharedProperties::stone)
            .properties(settings -> settings.nonOpaque())
            .transform(pickaxeOnly())
            .transform(BlockStressDefaults.setImpact(4))
            .defaultLoot()
            .item()
                .model((ctx, prov) ->
                        prov.withExistingParent(ctx.getName(), prov.modLoc("block/rotor/rotor_none")))
                .build()
            .lang("Generator Rotor")
            .register();

    public static final BlockEntry<CoilBlock> COIL = REGISTRATE.block("coil", CoilBlock::new)
            .blockstate((ctx, prov) ->
                    prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                        var builder = ConfiguredModel.builder();
                        if(state.get(CoilBlock.HAS_TERMINALS)) {
                            builder.modelFile(modModel(prov, "block/coil"));
                        } else {
                            builder.modelFile(modModel(prov, "block/coil_bare"));
                        }
                        switch(state.get(CoilBlock.FACING)) {
                            case DOWN -> builder.rotationX(180);
                            case NORTH -> builder.rotationX(90);
                            case SOUTH -> builder.rotationX(-90);
                            case EAST -> builder.rotationX(90).rotationY(90);
                            case WEST -> builder.rotationX(90).rotationY(-90);
                        }
                        return builder.build();
                    }))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .lang("Generator Coil")
            .register();

    public static final BlockEntry<GeneratorHousing> GENERATOR_HOUSING = REGISTRATE.block("generator_housing", GeneratorHousing::new)
            .blockstate((ctx, prov) ->
                    prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                        var builder = ConfiguredModel.builder().modelFile(modModel(prov, "block/generator_housing"));
                        int x = 0;
                        int y = 0;
                        var facing = state.get(GeneratorHousing.HORIZONTAL_FACING);
                        if(facing.getAxis() == Direction.Axis.X)
                            y = -90;
                        if(facing.getDirection() == Direction.AxisDirection.NEGATIVE)
                            x = -90;
                        if(state.get(GeneratorHousing.UP)) {
                            x = 90 - x;
                        }
                        return builder.rotationX(x).rotationY(y).build();
                    }))
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();

    public static final BlockEntry<LvSwitchBlock> LV_SWITCH = REGISTRATE.block("lv_switch", LvSwitchBlock::new)
            .blockstate((ctx, prov) ->
                    prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                        var builder = ConfiguredModel.builder();
                        var suffix = state.get(SwitchBlock.OPEN) ? "_off" : "_on";
                        surfaceFacingTransforms(state, (x, y, vertical) -> {
                            if(vertical) {
                                builder.modelFile(modModel(prov, "block/switches/lv_switch" + suffix + "_v"));
                            } else {
                                builder.modelFile(modModel(prov, "block/switches/lv_switch" + suffix + "_h"));
                            }
                            builder.rotationX(x).rotationY(y);
                        });
                        return builder.build();
                    }))
            .lang("LV Switch")
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/switches/lv_switch_off_v")))
                .build()
            .register();

    public static final BlockEntry<MvSwitchBlock> MV_SWITCH = REGISTRATE.block("mv_switch", MvSwitchBlock::new)
            .blockstate((ctx, prov) ->
                    prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                        var builder = ConfiguredModel.builder();
                        var suffix = state.get(SwitchBlock.OPEN) ? "_off" : "_on";
                        surfaceFacingTransforms(state, (x, y, vertical) -> {
                            if(vertical) {
                                builder.modelFile(modModel(prov, "block/switches/mv_switch" + suffix + "_v"));
                            } else {
                                builder.modelFile(modModel(prov, "block/switches/mv_switch" + suffix + "_h"));
                            }
                            builder.rotationX(x).rotationY(y);
                        });
                        return builder.build();
                    }))
            .lang("MV Switch")
            .item()
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/switches/mv_switch_off_v")))
            .build()
            .register();

    public static final BlockEntry<CreativeSourceBlock> CREATIVE_VOLTAGE_SOURCE = REGISTRATE.block("creative_voltage_source", CreativeSourceBlock::new)
            .blockstate(horizontalAxisBlock("block/creative_voltage_source"))
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();
    public static final BlockEntry<CreativeSourceBlock> CREATIVE_CURRENT_SOURCE = REGISTRATE.block("creative_current_source", CreativeSourceBlock::new)
            .blockstate(horizontalAxisBlock("block/creative_current_source"))
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();
    public static final BlockEntry<CreativeResistorBlock> CREATIVE_RESISTOR = REGISTRATE.block("creative_resistor", CreativeResistorBlock::new)
            .blockstate(horizontalAxisBlock("block/creative_resistor"))
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();

    public static final BlockEntry<LightFixtureBlock> LIGHT_FIXTURE = REGISTRATE.block("light_fixture", LightFixtureBlock::new)
            .blockstate((ctx, prov) ->
                    prov.getVariantBuilder(ctx.getEntry()).forAllStatesExcept(state -> {
                        var builder = ConfiguredModel.builder().modelFile(modModel(prov, "block/light_fixture"));
                        int x = 0, y = 0;
                        var facing = state.get(LightFixtureBlock.FACING);
                        switch(facing) {
                            case DOWN -> x = 180;
                            case NORTH -> x = 90;
                            case SOUTH -> x = -90;
                            case EAST -> { x = 90; y = 90; }
                            case WEST -> { x = 90; y = -90; }
                        }
                        if(!state.get(LightFixtureBlock.ALONG_FIRST_AXIS)) {
                            if(facing.getAxis() == Direction.Axis.Y)
                                y = 90;
                            // Other states would need a different model.
                        }
                        return builder.rotationX(x).rotationY(y).build();
                    }, LightFixtureBlock.POWER))
            .initialProperties(SharedProperties::wooden)
            .transform(axeOrPickaxe())
            .transform(LightFixtureBlock.setBulbModelOffset(0, 0.125f, 0))
            .defaultLoot()
            .simpleItem()
            .register();

    public static final BlockEntry<TransformerCoreBlock> TRANSFORMER_CORE = REGISTRATE.block("transformer_core", TransformerCoreBlock::new)
            .blockstate((ctx, prov) ->
                    prov.simpleBlockWithItem(ctx.getEntry(), prov.models().cubeAll(ctx.getName(), prov.modLoc("block/transformer/core"))))
            .initialProperties(SharedProperties::softMetal)
            .properties(properties -> properties.sounds(BlockSoundGroup.NETHERITE))
            .transform(pickaxeOnly())
            .defaultLoot()
            .simpleItem()
            .register();
    public static final BlockEntry<TransformerSmallBlock> TRANSFORMER_SMALL = REGISTRATE.block("transformer_small", TransformerSmallBlock::new)
            .initialProperties(() -> TRANSFORMER_CORE.get())
            .blockstate((ctx, prov) ->
                    prov.getMultipartBuilder(ctx.getEntry())
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small")).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.Z)
                        .end()
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small_coil1")).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.Z)
                        .condition(TransformerSmallBlock.COILS, 1, 2)
                        .end()
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small_coil2")).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.Z)
                        .condition(TransformerSmallBlock.COILS, 2)
                        .end()
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small")).rotationY(90).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                        .end()
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small_coil1")).rotationY(90).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                        .condition(TransformerSmallBlock.COILS, 1, 2)
                        .end()
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small_coil2")).rotationY(90).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                        .condition(TransformerSmallBlock.COILS, 2)
                        .end()
            )
            .loot((tables, block) -> tables.addDrop(block, TRANSFORMER_CORE.get()))
            .properties(properties -> properties.sounds(BlockSoundGroup.NETHERITE))
            .transform(pickaxeOnly())
            .register();
    public static final BlockEntry<TransformerMediumBlock> TRANSFORMER_MEDIUM = REGISTRATE.block("transformer_medium", TransformerMediumBlock::new)
            .initialProperties(() -> TRANSFORMER_CORE.get())
            .blockstate((ctx, prov) -> {
                var builder = prov.getMultipartBuilder(ctx.getEntry());
                transformerMedium(builder, prov, Direction.Axis.Z);
                transformerMedium(builder, prov, Direction.Axis.X);
            })
            .loot((tables, block) ->
                    tables.addDrop(block, tables.drops(TRANSFORMER_CORE.get())
                            .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(4)))
                    ))
            .properties(properties -> properties.sounds(BlockSoundGroup.NETHERITE))
            .transform(pickaxeOnly())
            .register();

    public static final BlockEntry<ElectricMotorBlock> ELECTRIC_MOTOR = REGISTRATE.block("electric_motor", ElectricMotorBlock::new)
            .blockstate(alternateDirectionalBlock(state -> switch(state.get(ElectricMotorBlock.FACING).getAxis()) {
                        case X, Z -> "block/electric_motor";
                        case Y -> "block/electric_motor_vertical";
                    }))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .transform(BlockStressDefaults.setCapacity(64))
            .transform(pickaxeOnly())
            .defaultLoot()
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/electric_motor_item")))
                .build()
            .register();

    public static final BlockEntry<ChemicalVatBlock> CHEMICAL_VAT = REGISTRATE.block("chemical_vat", ChemicalVatBlock::new)
            .blockstate((ctx, prov) ->
                    prov.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
                            ConfiguredModel.builder().modelFile(state.get(ChemicalVatBlock.OPEN) ?
                                    unchecked("chemical_vat_connected") : modModel(prov, "block/vat/closed"))
                                    .build()
                    ))
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.sounds(BlockSoundGroup.NETHERITE))
            .onRegister(CreateRegistrate.connectedTextures(ChemicalVatCTBehaviour::new))
            .transform(pickaxeOnly())
            .defaultLoot()
            .item()
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/vat/base")))
                .build()
            .register();

    public static final BlockEntry<Block> SILVER_ORE = REGISTRATE.block("silver_ore", Block::new)
            .defaultBlockstate()
            .initialProperties(() -> Blocks.GOLD_ORE)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.addDrop(b,
                    RegistrateBlockLootTables.dropsWithSilkTouch(b,
                            lt.applyExplosionDecay(b, ItemEntry.builder(ModdedItems.RAW_SILVER.get())
                                    .apply(ApplyBonusLootFunction.oreDrops(Enchantments.FORTUNE))))))
            .tag(BlockTags.NEEDS_IRON_TOOL, Tags.Blocks.ORES)
            .transform(TagGen.tagBlockAndItem("silver_ores", "ores_in_ground/stone"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<Block> DEEPSLATE_SILVER_ORE = REGISTRATE.block("deepslate_silver_ore", Block::new)
            .defaultBlockstate()
            .initialProperties(() -> Blocks.DEEPSLATE_GOLD_ORE)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.addDrop(b,
                    RegistrateBlockLootTables.dropsWithSilkTouch(b,
                            lt.applyExplosionDecay(b, ItemEntry.builder(ModdedItems.RAW_SILVER.get())
                                    .apply(ApplyBonusLootFunction.oreDrops(Enchantments.FORTUNE))))))
            .tag(BlockTags.NEEDS_IRON_TOOL, Tags.Blocks.ORES)
            .transform(TagGen.tagBlockAndItem("silver_ores", "ores_in_ground/deepslate"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalBlock(String model) {
        return (ctx, prov) -> {
            prov.horizontalBlock(ctx.getEntry(), modModel(prov, model));
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> alternateDirectionalBlock(String model) {
        return alternateDirectionalBlock($ -> model);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> alternateDirectionalBlock(Function<BlockState, String> modelProvider) {
        return (ctx, prov) -> prov.getVariantBuilder(ctx.getEntry())
                .forAllStates(state -> {
                    var builder = ConfiguredModel.builder().modelFile(modModel(prov, modelProvider.apply(state)));
                    switch(state.get(Properties.FACING)) {
                        case SOUTH -> builder.rotationY(180);
                        case EAST -> builder.rotationY(90);
                        case WEST -> builder.rotationY(-90);
                        case UP -> builder.rotationX(-90);
                        case DOWN -> builder.rotationX(90);
                    }
                    return builder.build();
                });
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalAxisBlock(String model) {
        return (ctx, prov) ->
                prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                    var builder = ConfiguredModel.builder().modelFile(modModel(prov, model));
                    if(state.get(Properties.HORIZONTAL_AXIS) == Direction.Axis.X)
                        builder.rotationY(90);
                    return builder.build();
                });
    }

    // This function needs two models. One for Y axis and one for other axis.
    public static void surfaceFacingTransforms(BlockState state, TriConsumer<Integer, Integer, Boolean> transformer) {
        var facing = state.get(Properties.FACING);
        var axis_along_first = state.get(CustomProperties.ALONG_FIRST_AXIS);

        int x = 0, y = 0;
        boolean verticalModel = false;
        switch(facing) {
            case UP -> { x = 180; verticalModel = true; }
            case DOWN -> verticalModel = true;
            case WEST -> y = 180;
            case NORTH -> y = -90;
            case SOUTH -> y = 90;
        };

        if(!axis_along_first) {
            if(verticalModel) {
                y = 90;
            } else {
                x = -90;
            }
        }

        transformer.accept(x, y, verticalModel);
    }

    public static ModelFile.ExistingModelFile modModel(RegistrateBlockstateProvider prov, String name) {
        return prov.models().getExistingFile(prov.modLoc(name));
    }

    private static ModelFile.UncheckedModelFile unchecked(String name) {
        return new ModelFile.UncheckedModelFile(PowerGrid.asResource(name));
    }

    private static void transformerMedium(MultiPartBlockStateBuilder builder, RegistrateBlockstateProvider prov, Direction.Axis axis) {
        transformerMediumPart(builder, prov, "block/transformer/medium_bottom", axis, 0).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_bottom", axis, 1).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_top", axis, 2).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_top", axis, 3).end();

        transformerMediumPart(builder, prov, "block/transformer/medium_coil1", axis, 0)
                .condition(TransformerMediumBlock.COILS, 1, 2).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_coil1", axis, 1)
                .condition(TransformerMediumBlock.COILS, 1, 2).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_coil2", axis, 2)
                .condition(TransformerMediumBlock.COILS, 2).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_coil2", axis, 3)
                .condition(TransformerMediumBlock.COILS, 2).end();
    }

    private static MultiPartBlockStateBuilder.PartBuilder transformerMediumPart(MultiPartBlockStateBuilder builder, RegistrateBlockstateProvider prov, String model, Direction.Axis axis, int part) {
        int y = 0;
        if(part % 2 == 1)
            y = 180;
        if(axis == Direction.Axis.X)
            y -= 90;
        return builder.part()
                .modelFile(modModel(prov, model))
                .rotationY(y)
                .addModel()
                .condition(TransformerMediumBlock.HORIZONTAL_AXIS, axis)
                .condition(TransformerMediumBlock.PART, part);
    }
}
