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
package org.patryk3211.powergrid.ponder.base;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.createmod.ponder.foundation.instruction.FadeOutOfSceneInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import org.patryk3211.powergrid.collections.ModdedDataComponents;
import net.minecraft.world.level.block.Block;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.PonderElectricNetwork;
import org.patryk3211.powergrid.electricity.light.string.PatternData;
import org.patryk3211.powergrid.electricity.light.string.StringLightCordEntity;
import org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBlockEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;
import org.patryk3211.powergrid.electricity.wire.powercord.CordEntity;
import org.patryk3211.powergrid.electricity.wire.powercord.ICordEndpoint;

import java.util.List;
import java.util.function.Function;

public class ElectricInstructions {
    public static final float DEFAULT_RESISTANCE = 0.005f;

    private final SceneBuilder builder;

    public ElectricInstructions(SceneBuilder builder) {
        this.builder = builder;
    }

    public static ElectricInstructions of(SceneBuilder builder) {
        return new ElectricInstructions(builder);
    }

    public void tickFor(int ticks) {
        builder.addInstruction(new ElectricityTickInstruction(ticks));
    }

    public void tickForever() {
        builder.addInstruction(scene -> scene.addElement(new PonderElement() {
            @Override
            public void tick(PonderScene scene) {
                PonderElectricNetwork.tickWorldNetworks(scene.getWorld());
            }

            @Override
            public boolean isVisible() {
                return false;
            }

            @Override
            public void setVisible(boolean visible) {

            }
        }));
    }

    public ElementLink<WireElement> connect(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, float resistance) {
        var link = new ElementLinkImpl<>(WireElement.class);
        var element = new WireElement(level -> {
            var wire = HangingWireEntity.create(level, new BlockWireEndpoint(pos1, terminal1), new BlockWireEndpoint(pos2, terminal2), ModdedItems.WIRE.asStack(), resistance);
            wire.updateCurveParams();
            return wire;
        });
        builder.addInstruction(new CreateWireInstruction(15, Direction.DOWN, element));
        builder.addInstruction(ponder -> ponder.linkElement(element, link));
        return link;
    }

    public ElementLink<WireElement> connect(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, DyeColor dye, float resistance) {
        var link = new ElementLinkImpl<>(WireElement.class);
        var element = new WireElement(level -> {
            var wire = HangingWireEntity.create(level, new BlockWireEndpoint(pos1, terminal1), new BlockWireEndpoint(pos2, terminal2), ModdedItems.INSULATED_COPPER_WIRE.asStack(), resistance);
            wire.setColor(dye);
            wire.updateCurveParams();
            return wire;
        });
        builder.addInstruction(new CreateWireInstruction(15, Direction.DOWN, element));
        builder.addInstruction(ponder -> ponder.linkElement(element, link));
        return link;
    }

    public ElementLink<WireElement> connectCord(ICordEndpoint endpoint1, ICordEndpoint endpoint2, float resistance) {
        var link = new ElementLinkImpl<>(WireElement.class);
        var element = new WireElement(level -> {
            var wire = CordEntity.create(level, endpoint1, endpoint2, ModdedItems.CORD.asStack(), resistance);
            wire.updateRenderParams();
            return wire;
        });
        builder.addInstruction(new CreateWireInstruction(15, Direction.DOWN, element));
        builder.addInstruction(ponder -> ponder.linkElement(element, link));
        return link;
    }

    public ElementLink<WireElement> connectLightCord(ICordEndpoint endpoint1, ICordEndpoint endpoint2, DyeColor[] colorPattern) {
        var link = new ElementLinkImpl<>(WireElement.class);
        var element = new WireElement(level -> {
            var stack = ModdedItems.STRING_LIGHT_CORD.asStack();
            if(colorPattern != null) {
                stack.set(ModdedDataComponents.LIGHT_PATTERN.get(), PatternData.of(List.of(colorPattern)));
            }
            var wire = StringLightCordEntity.create(level, endpoint1, endpoint2, stack, null);
            wire.updateRenderParams();
            return wire;
        });
        builder.addInstruction(new CreateWireInstruction(15, Direction.DOWN, element));
        builder.addInstruction(ponder -> ponder.linkElement(element, link));
        return link;
    }

    public ElementLink<WireElement> connect(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2) {
        return connect(pos1, terminal1, pos2, terminal2, DEFAULT_RESISTANCE);
    }

    public ElementLink<WireElement> connect(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, DyeColor dye) {
        return connect(pos1, terminal1, pos2, terminal2, dye, DEFAULT_RESISTANCE);
    }

    public ElementLink<WireElement> connectCord(ICordEndpoint endpoint1, ICordEndpoint endpoint2) {
        return connectCord(endpoint1, endpoint2, DEFAULT_RESISTANCE);
    }

    public ElementLink<WireElement> connectInvisible(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, float resistance) {
        var link = new ElementLinkImpl<>(WireElement.class);
        var element = new InvisibleWireElement(pos1, terminal1, pos2, terminal2, resistance);
        builder.addInstruction(new CreateWireInstruction(0, Direction.DOWN, element));
        builder.addInstruction(ponder -> ponder.linkElement(element, link));
        return link;
    }

    public ElementLink<WireElement> connectInvisible(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2) {
        return connectInvisible(pos1, terminal1, pos2, terminal2, DEFAULT_RESISTANCE);
    }

    public ElementLink<VoltageSource> addSource(BlockPos pos, int terminal, float voltage) {
        var link = new ElementLinkImpl<>(VoltageSource.class);
        var element = new VoltageSource(new BlockWireEndpoint(pos, terminal), voltage);
        builder.addInstruction(scene -> {
            scene.addElement(element);
            scene.linkElement(element, link);
        });
        return link;
    }

    public void connectPanels(BlockPos controller, BlockPos other) {
        builder.addInstruction(scene -> {
            var level = scene.getWorld();
            var be1 = level.getBlockEntity(controller);
            var be2 = level.getBlockEntity(other);
            if(be1 instanceof SolarPanelBlockEntity sbe1 && be2 instanceof SolarPanelBlockEntity sbe2)
                sbe1.connect(sbe2);
        });
    }

    public void mergePanels(BlockPos controller, BlockPos otherController) {
        builder.addInstruction(scene -> {
            var level = scene.getWorld();
            var be1 = level.getBlockEntity(controller);
            var be2 = level.getBlockEntity(otherController);
            if(be1 instanceof SolarPanelBlockEntity sbe1 && be2 instanceof SolarPanelBlockEntity sbe2) {
                SolarPanelBlockEntity.mergeMultiblock(sbe1, sbe2);
                var state = sbe1.getBlockState();
                level.setBlock(controller, state, Block.UPDATE_ALL);
            }
        });
    }

    public void splitPanels(BlockPos controller, int splitCoord, Direction splitPlane) {
        builder.addInstruction(scene -> {
            var level = scene.getWorld();
            var be1 = level.getBlockEntity(controller);
            if(be1 instanceof SolarPanelBlockEntity sbe1) {
                SolarPanelBlockEntity.splitMultiblock(sbe1, splitCoord, splitPlane);
                var state = sbe1.getBlockState();
                level.sendBlockUpdated(controller, state, state, Block.UPDATE_ALL);
            }
        });
    }

    public void setSource(ElementLink<VoltageSource> source, float value) {
        builder.addInstruction(scene -> {
            var element = scene.resolve(source);
            if(element != null)
                element.setValue(value);
        });
    }

    public void setSource(ElementLink<VoltageSource> source, Function<Integer, Float> func) {
        builder.addInstruction(scene -> {
            var element = scene.resolve(source);
            if(element != null)
                element.setFunction(func);
        });
    }

    public void removeWire(ElementLink<WireElement> wire) {
        builder.addInstruction(new FadeOutOfSceneInstruction<>(15, Direction.UP, wire));
    }

    public void setSource(BlockPos sourcePos, float value) {
        builder.addInstruction(new SetSourceInstruction(sourcePos, value));
    }

    public void unload() {
        builder.addInstruction(new UnloadElectricityWorldInstruction());
    }
}
