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
package org.patryk3211.powergrid.circuits.circuitboard;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableInt;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;

import java.util.*;
import java.util.function.Function;

public class BakedCircuit {
    public final List<OwnedFloatingNode> externalNodes = new ArrayList<>();
    public final List<INode> internalNodes = new ArrayList<>();
    public final List<AbstractElectricWire> wires = new ArrayList<>();
    public final List<TerminalBoundingBox> terminals = new ArrayList<>();
    public final List<ThermalUnit> thermalUnits = new ArrayList<>();
    public final List<PlacedComponent> tickedComponents = new ArrayList<>();
    private final Map<PlacedComponent, Function<Integer, FloatingNode>> padNodeProviderMap = new HashMap<>();

    private boolean isDamaged = false;
    private final CircuitBoardBlockEntity be;
    private boolean firstTick = true;

    protected BakedCircuit(CircuitBoardBlockEntity be) {
        this.be = be;
    }

    public FloatingNode getNode(CircuitSchematic.Node node) {
        return padNodeProviderMap.get(node.placed()).apply(node.pad());
    }

    private static void makePadNodes(BakedCircuit result, CircuitSchematic schematic, CircuitBoardBlockEntity be) {
        var pos = be.getBlockPos();
        var offset = Vec3.atLowerCornerOf(pos);
        for(var placed : schematic.components()) {
            var nodeIndexSet = new HashSet<Integer>();
            for(var pad : placed.footprint().getPads().values()) {
                if(pad.nodeIndex() >= 0)
                    nodeIndexSet.add(pad.nodeIndex());
            }
            var external = placed.component.emitExternalTerminals();
            int nodeOffset = external ? result.externalNodes.size() : result.internalNodes.size();
            for(var i = 0; i < nodeIndexSet.size(); ++i) {
                if(external) {
                    var node = new OwnedFloatingNode(new BlockWireEndpoint(pos, nodeOffset + i));
                    result.externalNodes.add(node);
                } else {
                    var node = new FloatingNode();
                    result.internalNodes.add(node);
                }
            }
            // Turns pad index into the corresponding component node.
            Function<Integer, FloatingNode> provider = external ?
                    index -> (FloatingNode) result.externalNodes.get(index + nodeOffset) :
                    index -> (FloatingNode) result.internalNodes.get(index + nodeOffset);
            result.padNodeProviderMap.put(placed, provider);

            var builder = new ComponentCircuitBuilder(pos, provider, result.internalNodes, result.wires);
            placed.nodes.clear();
            placed.wires.clear();

            MutableInt thermalIndex = new MutableInt(0);
            var thermalBuilders = new ArrayList<ThermalBuilder>();
            ThermalBuilder.IEmitter thermalEmitter = () -> {
                var thermalBuilder = new ThermalBuilder(placed.getUUID(), thermalIndex.getAndIncrement());
                thermalBuilders.add(thermalBuilder);
                return thermalBuilder;
            };
            placed.destroyed = false;
            placed.component.bake(placed, builder, thermalEmitter);
            var footprint = placed.footprint();
            var localPos =
                    new Vec3((placed.x + footprint.getWidth() * 0.5f) / 16f, 2 / 16f, (placed.y + footprint.getHeight() * 0.5f) / 16f)
                            .subtract(0.5, 0, 0.5)
                            .xRot((float) Math.PI * CircuitBoardBlock.getAngleX(be.getBlockState()) / 180f)
                            .yRot((float) Math.PI * CircuitBoardBlock.getAngleY(be.getBlockState()) / 180f)
                            .add(0.5, 0, 0.5)
                            .add(offset);
            thermalBuilders.stream()
                    .map(b -> b.build().withPosition(localPos))
                    .forEach(result.thermalUnits::add);
            result.tickedComponents.add(placed);

            if(external) {
                var bbs = placed.component.terminals(placed);
                bbs.stream().map(bb -> bb.offset(placed.x / 16f, 2 / 16f, placed.y / 16f)).forEach(result.terminals::add);
            }
        }
    }

    public static BakedCircuit from(CircuitSchematic schematic, CircuitBoardBlockEntity be) {
        var result = new BakedCircuit(be);

        // Create component pad nodes.
        makePadNodes(result, schematic, be);

        var bundles = schematic.findNodeBundles();
        for(var bundle : bundles) {
            if(bundle.size() <= 1) {
                // These shouldn't exist but just in case, discard them.
                continue;
            }
            if(bundle.size() == 2) {
                // Direct wire
                var iter = bundle.iterator();
                var node1 = iter.next();
                var node2 = iter.next();

                var R = node1.getPadResistance() + node2.getPadResistance();
                var wire = new ElectricWire(R, result.getNode(node1), result.getNode(node2));
                result.wires.add(wire);
            } else {
                // Junction between pads
                var junctionNode = new FloatingNode();
                result.internalNodes.add(junctionNode);

                for(var node : bundle) {
                    var wire = new ElectricWire(node.getPadResistance(), result.getNode(node), junctionNode);
                    result.wires.add(wire);
                }
            }
        }
        return result;
    }

    public void write(CompoundTag tag) {
        var thermalTag = new CompoundTag();
        for(var unit : thermalUnits) {
            unit.write(thermalTag);
        }
        tag.put("Thermal", thermalTag);
    }

    public void read(CompoundTag tag, boolean clientPacket) {
        if(tag.contains("Thermal")) {
            var thermalTag = tag.getCompound("Thermal");
            for(var unit : thermalUnits) {
                var overheated = unit.hasOverheated();
                unit.read(thermalTag);
                if(!overheated && unit.hasOverheated() && clientPacket) {
                    // Just destroyed
                    var world = be.getLevel();
                    if(world != null) {
                        // Spawn a spark explosion
                        var random = world.random;
                        var pos = unit.getPosition();
                        var x = (float) pos.x() + (random.nextFloat() - 0.5f) * 1 / 16f;
                        var y = (float) pos.y() + (random.nextFloat() - 0.5f) * 1 / 16f;
                        var z = (float) pos.z() + (random.nextFloat() - 0.5f) * 1 / 16f;
                        SparkParticleData.explodeParticles(world, x, y, z, Direction.UP, 10);
                        ModdedSoundEvents.COMPONENT_EXPLODE.playAt(world, pos, 1.0f, random.nextFloat() * 0.1f + 0.9f, true);
                        // Mark component as destroyed for rendering purposes
                        for(var placed : padNodeProviderMap.keySet()) {
                            if(placed.uuid.equals(unit.getId())) {
                                placed.destroyed = true;
                                Component.modelChanged(be.getBlockPos());
                            }
                        }
                    }
                }
                // Mark component as destroyed for rendering purposes
                for(var placed : padNodeProviderMap.keySet()) {
                    if(placed.uuid.equals(unit.getId())) {
                        if(clientPacket && placed.destroyed != unit.hasOverheated())
                            Component.modelChanged(be.getBlockPos());
                        placed.destroyed = unit.hasOverheated();
                    }
                }
            }
        }
        isDamaged = false;
        if(!clientPacket)
            return;
        var schematic = tag.getCompound("Schematic");
        if(schematic.contains("Components")) {
            for(var generic : schematic.getList("Components", Tag.TAG_COMPOUND)) {
                var compound = (CompoundTag) generic;
                var id = compound.getUUID("UUID");
                for(var placed : tickedComponents) {
                    if(!placed.uuid.equals(id))
                        continue;
                    var changed = false;
                    var tagProperties = compound.getCompound("Properties");
                    for(var property : placed.component.getProperties()) {
                        var tagEntry = tagProperties.get(property.id().toString());
                        var readValue = property.read(tagEntry);
                        if(tagEntry != null && !placed.get(property).equals(readValue)) {
                            // Value changed
                            placed.getEntry(property).setValueRaw(readValue);
                            changed = true;
                        }
                    }
                    if(changed) {
                        placed.stateUpdated();
                    }
                }
            }
        }
    }

    public boolean isDamaged() {
        if(isDamaged)
            return true;
        for(var unit : thermalUnits) {
            if(unit.hasOverheated()) {
                isDamaged = true;
                break;
            }
        }
        return isDamaged;
    }

    public void tick() {
        if(firstTick) {
            firstTick = false;
            return;
        }
        var client = be.getLevel().isClientSide;
        if(ThermalBehaviour.shouldOverheat()) {
            for (var unit : thermalUnits) {
                var overheated = unit.hasOverheated();
                if(!client)
                    unit.tick(be.coolingFactorMultiplier);
                if(client) {
                    var world = this.be.getLevel();
                    var random = world.random;
                    var pos = unit.getPosition();
                    var x = (float) pos.x() + (random.nextFloat() - 0.5f) * 1 / 16f;
                    var y = (float) pos.y() + (random.nextFloat() - 0.5f) * 1 / 16f;
                    var z = (float) pos.z() + (random.nextFloat() - 0.5f) * 1 / 16f;
                    if (!unit.hasOverheated() && unit.getTemperature() >= unit.getOverheatTemperature() - 50f) {
                        // Spawn particles
                        float chance = (unit.getTemperature() - unit.getOverheatTemperature() + 100) / 100;
                        if (random.nextFloat() < chance) {
                            world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
                        }
                    } else if (!overheated && unit.hasOverheated()) {
                    }
                } else {
                    if(!overheated && unit.hasOverheated()) {
                        // Server should sync overheat events
                        be.sendData();
                    }
                }
            }
        }
        // Ticks components as long as they return true
        tickedComponents.removeIf(placed -> !placed.tick());
    }
}
