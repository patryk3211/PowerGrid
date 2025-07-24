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

import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableInt;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class BakedCircuit {
    public final List<IElectricNode> externalNodes = new ArrayList<>();
    public final List<INode> internalNodes = new ArrayList<>();
    public final List<AbstractElectricWire> wires = new ArrayList<>();
    public final List<TerminalBoundingBox> terminals = new ArrayList<>();
    public final List<ThermalUnit> thermalUnits = new ArrayList<>();
    public final List<PlacedComponent> tickedComponents = new ArrayList<>();
    private final Map<PlacedComponent, Function<Integer, FloatingNode>> padNodeProviderMap = new HashMap<>();

    private boolean isDamaged = false;
    private final Supplier<World> world;

    protected BakedCircuit(Supplier<World> world) {
        this.world = world;
    }

    private FloatingNode getNode(CircuitSchematic.Node node) {
        return padNodeProviderMap.get(node.placed()).apply(node.pad());
    }

    public static BakedCircuit from(CircuitSchematic schematic, Supplier<World> world, BlockPos pos) {
        var result = new BakedCircuit(world);

        // Create component pad nodes.
        for(var placed : schematic.components()) {
            var nodeIndexSet = new HashSet<Integer>();
            for(var pad : placed.footprint().getPads().values()) {
                if(pad.nodeIndex() >= 0)
                    nodeIndexSet.add(pad.nodeIndex());
            }
            var external = placed.component.emitExternalTerminals();
            int nodeOffset = external ? result.externalNodes.size() : result.internalNodes.size();
            for(var i = 0; i < nodeIndexSet.size(); ++i) {
                var node = new FloatingNode();
                if(external) {
                    result.externalNodes.add(node);
                } else {
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
            placed.component.bake(placed, builder, thermalEmitter);
            var footprint = placed.footprint();
            var localPos = Vec3d.of(pos).add((placed.x + footprint.getWidth() * 0.5f) / 16f, 2 / 16f, (placed.y + footprint.getHeight() * 0.5f) / 16f);
            thermalBuilders.stream()
                    .map(b -> b.build().withPosition(localPos))
                    .forEach(result.thermalUnits::add);
            result.tickedComponents.add(placed);

            if(external) {
                var bbs = placed.component.terminals(placed);
                bbs.stream().map(bb -> bb.offset(placed.x / 16f, 2 / 16f, placed.y / 16f)).forEach(result.terminals::add);
            }
        }

        var bundles = schematic.findNodeBundles();
        for(var bundle : bundles) {
            if(bundle.size() <= 1) {
                // These shouldn't exist but just in case, discard them.
                continue;
            }
            if(bundle.size() == 2) {
                // Direct wire
                var nodes = bundle.toArray(CircuitSchematic.Node[]::new);
                var node1 = result.getNode(nodes[0]);
                var node2 = result.getNode(nodes[1]);

                var R = nodes[0].getPadResistance() + nodes[1].getPadResistance();
                var wire = new ElectricWire(R, node1, node2);
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

    public void write(NbtCompound tag) {
        var thermalTag = new NbtCompound();
        for(var unit : thermalUnits) {
            unit.write(thermalTag);
        }
        tag.put("Thermal", thermalTag);
    }

    public void read(NbtCompound tag) {
        if(tag.contains("Thermal")) {
            var thermalTag = tag.getCompound("Thermal");
            for(var unit : thermalUnits) {
                unit.read(thermalTag);
            }
        }
        isDamaged = false;
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
        var client = world.get().isClient;
        for(var unit : thermalUnits) {
            var overheated = unit.hasOverheated();
            unit.tick();
            if(client) {
                var world = this.world.get();
                var random = world.random;
                var pos = unit.getPosition();
                var x = (float) pos.getX() + (random.nextFloat() - 0.5f) * 1 / 16f;
                var y = (float) pos.getY() + (random.nextFloat() - 0.5f) * 1 / 16f;
                var z = (float) pos.getZ() + (random.nextFloat() - 0.5f) * 1 / 16f;
                if(!unit.hasOverheated() && unit.getTemperature() >= unit.getOverheatTemperature() - 50f) {
                    // Spawn particles
                    float chance = (unit.getTemperature() - unit.getOverheatTemperature() + 100) / 100;
                    if (random.nextFloat() < chance) {
                        world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
                    }
                } else if(!overheated && unit.hasOverheated()) {
                    // Spawn a spark explosion
                    SparkParticleData.explodeParticles(world, x, y, z, Direction.UP, 10);
                    ModdedSoundEvents.COMPONENT_EXPLODE.playAt(world, pos, 1.0f, random.nextFloat() * 0.1f + 0.9f, true);
                }
            }
        }
        // Ticks components as long as they return true
        tickedComponents.removeIf(placed -> !placed.tick());
    }
}
