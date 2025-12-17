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
package org.patryk3211.powergrid.utility;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableBlock;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchBlock;
import org.patryk3211.powergrid.electricity.fuse.FuseHolderBlock;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerSmallBlock;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.VerticalCommutatorBlock;
import org.patryk3211.powergrid.kinetics.generator.rotor.AbstractRotorBlock;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock;

import java.util.function.Function;

public class DataProviderUtility {
    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalBlock(String model) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalBlock(Function<BlockState, String> model) {
        throw new AssertionError();
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> alternateDirectionalBlock(String model) {
        return alternateDirectionalBlock($ -> model);
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> alternateDirectionalBlock(Function<BlockState, String> modelProvider) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalAxisBlock(String model) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> surfaceSwitch(String baseName) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> surfaceBlock(String baseName) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static NonNullBiConsumer<DataGenContext<Block, LightFixtureBlock>, RegistrateBlockstateProvider> lightFixture(String baseName) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> housing(String name) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> basinHeater(String baseName) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends AbstractRotorBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> rotorModel(String name) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends VerticalCommutatorBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> verticalCommutator(String name) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends WindingBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> windingModel() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends HvSwitchBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> hvSwitch(String baseName) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static NonNullBiConsumer<DataGenContext<Block, TransformerSmallBlock>, RegistrateBlockstateProvider> transformerSmall() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static NonNullBiConsumer<DataGenContext<Block, TransformerMediumBlock>, RegistrateBlockstateProvider> transformerMedium() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> downFacing(String name) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> upFacing(String name) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static NonNullBiConsumer<DataGenContext<Block, CircuitBoardBlock>, RegistrateBlockstateProvider> circuitBoard() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static NonNullBiConsumer<DataGenContext<Block, CircuitDesignTableBlock>, RegistrateBlockstateProvider> circuitDesignTable() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static NonNullBiConsumer<DataGenContext<Block, FuseHolderBlock>, RegistrateBlockstateProvider> fuseHolder() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> cubeColumn(String side, String end) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> cubeAllWithItem(String name) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> simple(String model) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> carbonPile(String baseName) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> generated() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> gauge(String model, String texture) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> itemWithParent(String parent) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> barrier() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> northFacing(String name) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> air() {
        throw new AssertionError();
    }
}
