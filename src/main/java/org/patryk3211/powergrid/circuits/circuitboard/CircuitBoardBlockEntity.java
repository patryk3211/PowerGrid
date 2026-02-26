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
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.createmod.catnip.math.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.ViaComponent;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.ISchematicHolder;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.wire.CircuitBoardEndpoint;
import org.patryk3211.powergrid.network.packets.EndpointTrackingC2SPacket;
import org.patryk3211.powergrid.utility.ClientSideAccess;
import org.patryk3211.powergrid.utility.Lang;

import java.util.*;

import static org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock.HORIZONTAL_FACING;
import static org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock.ROTATION;

public class CircuitBoardBlockEntity extends ElectricBlockEntity implements IElectric, IHaveGoggleInformation, ISchematicHolder, ElectricBehaviour.SyncAppender {
    private CircuitSchematic schematic = new CircuitSchematic();
    private BakedCircuit baked;
    private final Map<Class<?>, Collection<PlacedComponent>> componentCache = new HashMap<>();
    private final Map<CircuitBoardBlockEntity, List<ElectricWire>> edgeViadWires = new HashMap<>();

    private final Map<AirCurrent, Float> coolingAir = new HashMap<>();
    public float totalCoolingFactorMultiplier = 1.0f;

    @Nullable
    @Environment(EnvType.CLIENT)
    public CircuitBoardModelQuads quads;

    public CircuitBoardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour.setSyncAppender(this);
    }

    @Override
    public void setSchematic(CircuitSchematic schematic) {
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
        baked.read(tag, false);
        if(!level.isClientSide)
            notifyUpdate();
    }

    @Nullable
    private FloatingNode getViaAt(Direction sideIn, int edgePosition, Orientation edgeOut) {
        if(baked == null)
            return null;
        var orientation = CircuitBoardBlock.getOrientation(getBlockState(), sideIn);
        if(orientation == null)
            return null;
        if(((edgeOut.isX() && orientation.isX()) || (edgeOut.isY() && orientation.isY())) == (edgeOut.positive() == orientation.positive())) {
            edgePosition = 15 - edgePosition;
        }
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

    private ElectricalNetwork unifyNetwork(ElectricBehaviour eb1, IElectricNode node1, ElectricBehaviour eb2, IElectricNode node2) {
        var net1 = node1.getNetwork();
        var net2 = node2.getNetwork();
        ElectricalNetwork network;
        if(net1 == null && net2 == null) {
            network = GlobalElectricNetworks.getWorldNetworks(level).newNetwork();
            eb1.tracedAdd(network, node1);
            eb2.tracedAdd(network, node2);
        } else if(net1 == null) {
            network = net2;
            eb1.tracedAdd(net2, node1);
        } else if(net2 == null) {
            network = net1;
            eb2.tracedAdd(net1, node2);
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

    private ElectricWire makeWire(ElectricBehaviour eb2, IElectricNode outNode, IElectricNode hereNode) {
        var wire = new ElectricWire(0.002f, outNode, hereNode);
        if(!electricBehaviour.isPaused() && !eb2.isPaused()) {
            var network = unifyNetwork(electricBehaviour, hereNode, eb2, outNode);
            network.addWire(wire);
        }
        return wire;
    }

    private void processViaOrientation(@NotNull CircuitBoardBlockEntity be, Orientation expectedOrientation, PlacedComponent placed, Orientation edge, int position) {
        if(edge != expectedOrientation)
            return;
        var dir = CircuitBoardBlock.getDirection(getBlockState(), edge);
        var viaNode = be.getViaAt(dir.getOpposite(), position, edge);
        var thisViaNode = baked.getNode(new CircuitSchematic.Node(placed, 0));
        if(viaNode == null)
            return;
        if(thisViaNode == null)
            return;

        var wire = makeWire(be.electricBehaviour, viaNode, thisViaNode);
        edgeViadWires.computeIfAbsent(be, $ -> new ArrayList<>()).add(wire);
        be.edgeViadWires.computeIfAbsent(this, $ -> new ArrayList<>()).add(wire);
    }

    private void processNeighbor(@NotNull CircuitBoardBlockEntity be, Orientation expectedOrientation) {
        for(var placed : getComponents(ViaComponent.class)) {
            // This must also take corners into account (a via on two edges)
            if(placed.x == 0) {
                processViaOrientation(be, expectedOrientation, placed, Orientation.LEFT, placed.y);
            }
            if(placed.y == 0) {
                processViaOrientation(be, expectedOrientation, placed, Orientation.UP, placed.x);
            }
            if(placed.x == 15) {
                processViaOrientation(be, expectedOrientation, placed, Orientation.RIGHT, placed.y);
            }
            if(placed.y == 15) {
                processViaOrientation(be, expectedOrientation, placed, Orientation.DOWN, placed.x);
            }
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
        if(level != null) {
            var changedExternal = electricBehaviour.getExternalNodes().size() != baked.externalNodes.size();
            if (changedExternal)
                electricBehaviour.breakConnections();
        }
        electricBehaviour.rebuildCircuit(true);
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
        if(level.isClientSide) {
            // Layer position doesn't matter here.
            ModdedPackets.sendToServer(new EndpointTrackingC2SPacket(new CircuitBoardEndpoint(worldPosition, 0, 0), false));
        }
    }

    @Override
    public void tick() {
        super.tick();
        var iter = coolingAir.entrySet().iterator();
        while(iter.hasNext()) {
            var entry = iter.next();
            if(entry.getKey().source.isSourceRemoved() || entry.getKey().source.getSpeed() == 0) {
                totalCoolingFactorMultiplier -= entry.getValue();
                iter.remove();
            }
        }
        if(baked != null) {
            baked.tick();
            setUnsaved();
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
                for(var wire : entry.getValue()) {
                    var node1 = wire.getNode1();
                    var node2 = wire.getNode2();
                    ElectricalNetwork network;
                    if(electricBehaviour.getInternalNodes().contains(node1)) {
                        // Node1 is hereNode
                        network = unifyNetwork(electricBehaviour, node1, other, node2);
                    } else {
                        // Node2 is hereNode
                        network = unifyNetwork(electricBehaviour, node2, other, node1);
                    }
                    network.addWire(wire);
                }
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.put("Schematic", schematic.serializeNbt());
        if(baked != null)
            baked.write(tag);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        if(!tag.contains("Schematic")) {
            level.destroyBlock(worldPosition, false);
            return;
        }
        super.read(tag, registries, clientPacket);
        if(!clientPacket || tag.getBoolean("Rebuild") || baked == null) {
            schematic.deserializeNbt(tag.getCompound("Schematic"));
            bakeCircuit();
        }
        if(baked != null)
            baked.read(tag, clientPacket);
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
        if(index < 0 || index >= baked.terminals.size())
            return null;
        var terminal = baked.terminals.get(index);
        return terminal
                .rotateAroundX(-CircuitBoardBlock.getAngleX(state))
                .rotateAroundY(-CircuitBoardBlock.getAngleY(state));
    }

    @Override
    public CircuitSchematic getSchematic() {
        return schematic;
    }

    @Override
    public @NotNull String getSchematicName() {
        var name = schematic.getName();
        return name == null ? "missing" : name;
    }

    @Override
    public void setSchematicName(String name) {
        schematic.setName(name);
        notifyUpdate();
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
    public void invalidate() {
        super.invalidate();
        if(level.isClientSide) {
            // Layer position doesn't matter here.
            ModdedPackets.sendToServer(new EndpointTrackingC2SPacket(new CircuitBoardEndpoint(worldPosition, 0, 0), true));
        }
    }

    @Override
    public void remove() {
        disconnectViad();
        super.remove();
    }

    @Override
    public boolean addToGoggleTooltip(List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        if(baked == null)
            return false;

        var hasInfo = new MutableBoolean(false);
        if(baked.isDamaged()) {
            Lang.translate("gui.damage_header")
                    .forGoggles(tooltip);
            Lang.translate("gui.circuit_board.damage_body")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                var player = ClientSideAccess.player();
                if (!player.isCreative())
                    return;
                Lang.translate("gui.circuit_board.damage_creative_repair")
                        .style(ChatFormatting.DARK_GRAY)
                        .forGoggles(tooltip);
            });
            hasInfo.setTrue();
        }
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            var hit = ClientSideAccess.getHitResult();
            var hitLocalPos = hit.getLocation().subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
            hitLocalPos = VecHelper.rotateCentered(hitLocalPos, -CircuitBoardBlock.getAngleY(getBlockState()), Direction.Axis.Y);
            hitLocalPos = VecHelper.rotateCentered(hitLocalPos, -CircuitBoardBlock.getAngleX(getBlockState()), Direction.Axis.X);
            for(var placed : getComponents(IComponentGoggleInformation.class)) {
                if(hitLocalPos.x * 16 >= placed.x && hitLocalPos.z * 16 >= placed.y &&
                    hitLocalPos.x * 16 < placed.x + placed.footprint().getWidth() &&
                    hitLocalPos.z * 16 < placed.y + placed.footprint().getHeight()) {
                    var info = (IComponentGoggleInformation) placed.component;
                    if (info.addToGoggleTooltip(placed, tooltip, isPlayerSneaking)) {
                        hasInfo.setTrue();
                    }
                }
            }
        });
        return hasInfo.getValue();
    }

    public BakedCircuit getBaked() {
        return baked;
    }

    public void addCoolingMultiplier(AirCurrent current, float value) {
        var currentValue = coolingAir.get(current);
        if(currentValue != null) {
            totalCoolingFactorMultiplier -= currentValue;
            if (totalCoolingFactorMultiplier < 1)
                totalCoolingFactorMultiplier = 1;
        }
        coolingAir.put(current, value);
        totalCoolingFactorMultiplier += value;
    }

    public void removeCoolingMultiplier(AirCurrent current) {
        var currentValue = coolingAir.remove(current);
        if(currentValue != null)
            totalCoolingFactorMultiplier -= currentValue;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition);
    }

    public void repairBroken() {
        if(baked != null && baked.isDamaged()) {
            // This repairs all components
            bakeCircuit();
            notifyUpdate();
        }
    }

    @Override
    public void writeToSync(FriendlyByteBuf buffer) {
        if(baked == null)
            return;
        for(var unit : baked.thermalUnits) {
            buffer.writeFloat(unit.getTemperature());
        }
    }

    @Override
    public void readFromSync(FriendlyByteBuf buffer) {
        if(baked == null)
            return;
        for(var unit : baked.thermalUnits) {
            unit.setTemperature(buffer.readFloat());
        }
    }
}
