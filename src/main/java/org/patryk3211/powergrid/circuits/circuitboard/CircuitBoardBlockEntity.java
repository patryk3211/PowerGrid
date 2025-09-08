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

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ViaComponent;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.utility.Lang;

import java.util.*;

import static org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock.HORIZONTAL_FACING;
import static org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock.ROTATION;

public class CircuitBoardBlockEntity extends ElectricBlockEntity implements IElectric, IHaveGoggleInformation {
    private CircuitSchematic schematic = new CircuitSchematic();
    private BakedCircuit baked;
    private final Map<Class<?>, Collection<PlacedComponent>> componentCache = new HashMap<>();
    private final Map<CircuitBoardBlockEntity, List<ElectricWire>> edgeViadWires = new HashMap<>();

    private AirCurrent coolingAir;
    protected float coolingFactorMultiplier = 1;

    public CircuitBoardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void withSchematic(CircuitSchematic schematic) {
        if(level.isClientSide)
            return;
        if(schematic == null) {
            level.destroyBlock(worldPosition, false);
            return;
        }
        this.schematic = new CircuitSchematic(schematic);
        bakeCircuit();
        notifyUpdate();
    }

    public void setAdditionalData(CompoundTag tag) {
        if(baked == null)
            return;
        baked.read(tag);
        notifyUpdate();
    }

    @Nullable
    private FloatingNode getViaAt(Direction sideIn, int edgePosition) {
        if(baked == null)
            return null;
        var orientation = CircuitBoardBlock.getOrientation(getBlockState(), sideIn);
        if(orientation == null)
            return null;
        for(var placed : getComponents(ViaComponent.class)) {
            var match = switch(orientation) {
                case UP -> placed.x == edgePosition && placed.y == 0;
                case DOWN -> placed.x == edgePosition && placed.y == 15;
                case LEFT -> placed.x == 0 && placed.y == edgePosition;
                case RIGHT -> placed.x == 15 && placed.y == edgePosition;
            };
            if(match)
                return baked.getNode(new CircuitSchematic.Node(placed, 0));
        }
        return null;
    }

    private ElectricalNetwork unifyNetwork(ElectricBehaviour other) {
        var net1 = electricBehaviour.getNetwork();
        var net2 = other.getNetwork();
        ElectricalNetwork network;
        if(net1 == null && net2 == null) {
            network = GlobalElectricNetworks.getWorldNetworks(level).newNetwork();
            electricBehaviour.joinNetwork(network);
            other.joinNetwork(network);
        } else if(net1 == null) {
            network = net2;
            electricBehaviour.joinNetwork(network);
        } else if(net2 == null) {
            network = net1;
            other.joinNetwork(network);
        } else if(net1 != net2) {
            if(net1.size() >= net2.size()) {
                network = net1;
                network.merge(net2);
            } else {
                network = net2;
                network.merge(net1);
            }
        } else {
            network = net1;
        }
        return network;
    }

    private ElectricWire makeWire(ElectricBehaviour eb2, IElectricNode node1, IElectricNode node2) {
        var network = unifyNetwork(eb2);
        var wire = new ElectricWire(0.002f, node1, node2);
        network.addWire(wire);
        return wire;
    }

    private void processNeighbor(@NotNull CircuitBoardBlockEntity be, Orientation expectedOrientation) {
        for(var placed : getComponents(ViaComponent.class)) {
            // Which edge
            Orientation edge;
            int position;
            if(placed.x == 0) {
                edge = Orientation.LEFT;
                position = placed.y;
            } else if(placed.y == 0) {
                edge = Orientation.UP;
                position = placed.x;
            } else if(placed.x == 15) {
                edge = Orientation.RIGHT;
                position = placed.y;
            } else if(placed.y == 15) {
                edge = Orientation.DOWN;
                position = placed.x;
            } else {
                // Not on edge
                continue;
            }
            if(edge != expectedOrientation)
                continue;
            var dir = CircuitBoardBlock.getDirection(getBlockState(), edge);
            var viaNode = be.getViaAt(dir.getOpposite(), position);
            var thisViaNode = baked.getNode(new CircuitSchematic.Node(placed, 0));

            var wire = makeWire(be.electricBehaviour, viaNode, thisViaNode);
            edgeViadWires.computeIfAbsent(be, $ -> new ArrayList<>()).add(wire);
            be.edgeViadWires.computeIfAbsent(this, $ -> new ArrayList<>()).add(wire);
        }
    }

    private void disconnectViad() {
        for(var entry : edgeViadWires.entrySet()) {
            entry.getKey().edgeViadWires.remove(this);
            entry.getValue().forEach(ElectricWire::remove);
        }
        edgeViadWires.clear();
    }

    private void bakeCircuit() {
        componentCache.clear();
        disconnectViad();
        baked = BakedCircuit.from(schematic, this);
        for(var placed : schematic.components()) {
            placed.withWorld(this::getLevel, worldPosition);
        }
        electricBehaviour.rebuildCircuit();
        if(level != null) {
            if(level.isClientSide)
                Component.modelChanged(worldPosition);
            edgeConnect();
        }
    }

    private void edgeConnect() {
        disconnectViad();
        var state = getBlockState();
        for(var orientation : Orientation.values()) {
            var dir = CircuitBoardBlock.getDirection(state, orientation);
            var opt = level.getBlockEntity(worldPosition.relative(dir), ModdedBlockEntities.CIRCUIT_BOARD.get());
            if(opt.isEmpty())
                continue;
            var be = opt.get();
            var neighborState = be.getBlockState();
            if(state.getValue(ROTATION) == 1) {
                if(neighborState.getValue(ROTATION) == 1 &&
                        state.getValue(HORIZONTAL_FACING) == neighborState.getValue(HORIZONTAL_FACING))
                    processNeighbor(be, orientation);
            } else if(state.getValue(ROTATION).equals(neighborState.getValue(ROTATION))) {
                processNeighbor(be, orientation);
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        edgeConnect();
    }

    @Override
    public void tick() {
        super.tick();
        if(coolingAir != null && (coolingAir.source.isSourceRemoved() || coolingAir.source.getSpeed() == 0)) {
            noCooling();
        }
        if(baked != null) {
            baked.tick();
            if(!level.isClientSide)
                setChanged();
        }
    }

    @Override
    public void paused() {
        for(var entry : edgeViadWires.entrySet()) {
            var other = entry.getKey().electricBehaviour;
            if(!other.isPaused()) {
                // First paused, must remove shared wires.
                entry.getValue().forEach(ElectricWire::remove);
            }
        }
    }

    @Override
    public void unpaused() {
        for(var entry : edgeViadWires.entrySet()) {
            var other = entry.getKey().electricBehaviour;
            if(!other.isPaused()) {
                // Both are unpaused, can add shared wires.
                var network = unifyNetwork(other);
                entry.getValue().forEach(network::addWire);
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        tag.put("Schematic", schematic.serializeNbt());
        if(baked != null)
            baked.write(tag);
        super.write(tag, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        if(!tag.contains("Schematic")) {
            level.destroyBlock(worldPosition, false);
            return;
        }
        super.read(tag, clientPacket);
        if(!clientPacket || tag.getBoolean("Rebuild") || baked == null) {
            schematic.deserializeNbt(tag.getCompound("Schematic"));
            bakeCircuit();
        }
        if(baked != null)
            baked.read(tag);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        if(baked != null)
            builder.setTo(baked);
    }

    @Override
    public int terminalCount() {
        return baked == null ? 0 : baked.externalNodes.size();
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        if(baked == null)
            return null;
        var terminal = baked.terminals.get(index);
        return terminal
                .rotateAroundX(-CircuitBoardBlock.getAngleX(state))
                .rotateAroundY(-CircuitBoardBlock.getAngleY(state));
    }

    public CircuitSchematic getSchematic() {
        return schematic;
    }

    public <T> Collection<PlacedComponent> getComponents(Class<T> ofClass) {
        if(componentCache.containsKey(ofClass))
            return componentCache.get(ofClass);
        var components = new ArrayList<PlacedComponent>();
        for(var placed : schematic.components()) {
            if(ofClass.isInstance(placed.component))
                components.add(placed);
        }
        componentCache.put(ofClass, components);
        return components;
    }

    @Override
    public void remove() {
        disconnectViad();
        super.remove();
    }

    @Override
    public boolean addToGoggleTooltip(List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        if(baked == null || !baked.isDamaged())
            return false;

        Lang.translate("gui.circuit_board.damage_header")
                .forGoggles(tooltip);
        Lang.translate("gui.circuit_board.damage_body")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        return true;
    }

    public BakedCircuit getBaked() {
        return baked;
    }

    public void setCoolingMultiplier(AirCurrent current, float value) {
        coolingFactorMultiplier = value;
        coolingAir = current;
    }

    public void noCooling() {
        coolingFactorMultiplier = 1;
        coolingAir = null;
    }
}
