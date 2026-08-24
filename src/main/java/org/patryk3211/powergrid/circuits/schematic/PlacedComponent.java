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
package org.patryk3211.powergrid.circuits.schematic;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.PropertyEntry;
import org.patryk3211.powergrid.collections.ModPackets;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.network.PlayerSelection;
import org.patryk3211.powergrid.network.packets.UpdateComponentBiPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_TO_GRID_SCALE;

public class PlacedComponent {
    public final Component component;
    public final int x;
    public final int y;
    public final UUID uuid;
    private final List<PropertyEntry<?>> properties = new ArrayList<>();

    private Supplier<Level> worldSupplier;
    private BlockPos pos;

    public final List<INode> nodes = new ArrayList<>();
    public final List<AbstractElectricWire> wires = new ArrayList<>();

    public Object customData;
    public boolean destroyed;

    public PlacedComponent(CompoundTag tag, int version) {
        this(get(tag.getString("Id")), tag.getInt("X"), tag.getInt("Y"), tag.contains("UUID") ? tag.getUUID("UUID") : null);
        if(CircuitSchematic.VERSION != version)
            component.dataFixup(tag, version);
        var propertyMap = tag.getCompound("Properties");
        for(var entry : properties) {
            entry.read(propertyMap);
        }
    }

    public PlacedComponent(PlacedComponent placed) {
        this(placed.component, placed.x, placed.y, placed.uuid);
        this.worldSupplier = placed.worldSupplier;
        this.pos = placed.pos;
        this.destroyed = placed.destroyed;
        this.customData = placed.customData;
        for(var property : properties) {
            property.setValueRaw(placed.get(property.property));
        }
    }

    public PlacedComponent(PlacedComponent placed, int x, int y) {
        this(placed.component, x, y, UUID.randomUUID());
        this.worldSupplier = placed.worldSupplier;
        for(var property : properties) {
            property.setValueRaw(placed.get(property.property));
        }
    }

    private static Component get(String id) {
        return ComponentRegistry.get(ResourceLocation.parse(id));
    }

    public PlacedComponent(Component component, int x, int y, UUID uuid) {
        this.component = component;
        this.x = x;
        this.y = y;
        this.uuid = uuid == null ? UUID.randomUUID() : uuid;
        for(var property : component.getProperties()) {
            properties.add(PropertyEntry.makeFor(property, this));
        }
    }

    public void withWorld(Supplier<Level> worldSupplier, BlockPos pos) {
        this.worldSupplier = worldSupplier;
        this.pos = pos;
    }

    public Level getWorld() {
        return worldSupplier.get();
    }

    public boolean isClient() {
        return getWorld().isClientSide;
    }

    public void onClientWorld(Supplier<Consumer<Level>> callback) {
        var world = getWorld();
        if(world.isClientSide)
            callback.get().accept(world);
    }

    public void onServerWorld(Supplier<Consumer<Level>> callback) {
        var world = getWorld();
        if(!world.isClientSide)
            callback.get().accept(world);
    }

    public CompoundTag serializeNbt() {
        var tag = new CompoundTag();

        var id = ComponentRegistry.getId(component);
        tag.putString("Id", id.toString());
        tag.putInt("X", x);
        tag.putInt("Y", y);
        tag.putUUID("UUID", uuid);

        if(!properties.isEmpty()) {
            var propertyMap = new CompoundTag();
            for(var entry : properties) {
                entry.write(propertyMap);
            }
            tag.put("Properties", propertyMap);
        }

        return tag;
    }

    public Tag serializeSafeNbt() {
        var tag = new CompoundTag();

        var id = ComponentRegistry.getId(component);
        tag.putString("Id", id.toString());
        tag.putInt("X", x);
        tag.putInt("Y", y);

        if(!properties.isEmpty()) {
            var propertyMap = new CompoundTag();
            for(var entry : properties) {
                if(entry.property.isUnsafe())
                    continue;
                entry.write(propertyMap);
            }
            tag.put("Properties", propertyMap);
        }

        return tag;
    }

    public ComponentFootprint footprint() {
        return component.footprint(this);
    }

    public boolean canPlace(int x, int y) {
        return component.canPlace(this, x, y);
    }

    public boolean tick() {
        return component.tick(this);
    }

    public void stateUpdated() {
        component.stateUpdated(this);
    }

    public void notifyClients(ComponentProperty<?> property) {
        onServerWorld(() -> world -> {
            var circuit = (CircuitBoardBlockEntity) world.getBlockEntity(pos);
            if(circuit == null)
                return;
            ModPackets.PACKETS.sendTo(PlayerSelection.tracking(circuit), new UpdateComponentBiPacket(circuit, this, property));
        });
    }

    public void notifyClients(ResourceLocation propertyId) {
        onServerWorld(() -> world -> {
            var circuit = (CircuitBoardBlockEntity) world.getBlockEntity(pos);
            if(circuit == null)
                return;
            ModPackets.PACKETS.sendTo(PlayerSelection.tracking(circuit), new UpdateComponentBiPacket(circuit, this, propertyId));
        });
    }

    public <T> T get(ComponentProperty<T> property) {
        for(var entry : properties) {
            if(entry.property == property)
                return (T) entry.get();
        }
        throw new IllegalArgumentException("Placed components doesn't have the requested property");
    }

    public boolean has(ComponentProperty<?> property) {
        for(var entry : properties) {
            if(entry.property == property)
                return true;
        }
        return false;
    }

    public PropertyEntry<?> getEntry(ResourceLocation id) {
        for(var entry : properties) {
            if(entry.property.id().equals(id)) {
                return entry;
            }
        }
        throw new IllegalArgumentException("Placed components doesn't have the requested property");
    }

    public <T> PropertyEntry<T> getEntry(ComponentProperty<T> property) {
        for(var entry : properties) {
            if(entry.property == property)
                return (PropertyEntry<T>) entry;
        }
        throw new IllegalArgumentException("Placed components doesn't have the requested property");
    }

    public <T> void set(ComponentProperty<T> property, T value) {
        for(var entry : properties) {
            if(entry.property == property) {
                entry.setValueRaw(value);
                return;
            }
        }
        throw new IllegalArgumentException("Placed components doesn't have the requested property");
    }

    public String getString(ComponentProperty<?> property) {
        for(var entry : properties) {
            if(entry.property == property)
                return entry.stringValue();
        }
        throw new IllegalArgumentException("Placed components doesn't have the requested property");
    }

    public void setString(ComponentProperty<?> property, String value) {
        for(var entry : properties) {
            if(entry.property == property) {
                entry.setValue(value);
                return;
            }
        }
        throw new IllegalArgumentException("Placed components doesn't have the requested property");
    }

    public boolean intersects(int x, int y, int width, int height) {
        var footprint = footprint();
        return this.x < x + width && this.x + footprint.getWidth() > x &&
                this.y < y + height && this.y + footprint.getHeight() > y;
    }

    public boolean intersects32(int x, int y, int width, int height) {
        var footprint = footprint();
        return this.x * GRID_TO_GRID_SCALE < x + width && this.x * GRID_TO_GRID_SCALE + footprint.getWidth() * GRID_TO_GRID_SCALE > x &&
                this.y * GRID_TO_GRID_SCALE < y + height && this.y * GRID_TO_GRID_SCALE + footprint.getHeight() * GRID_TO_GRID_SCALE > y;
    }

    public void add(INode node) {
        nodes.add(node);
    }

    public void add(AbstractElectricWire wire) {
        wires.add(wire);
    }

    public UUID getUUID() {
        return uuid;
    }

    public BlockPos getPos() {
        return pos;
    }

    public @NotNull Vec3 getExactPos() {
        var pos = new Vec3((x + 0.5f) / 16f, 2 / 16f, (y + 0.5f) / 16f);
        var state = getWorld().getBlockState(this.pos);
        pos = VecHelper.rotateCentered(pos, CircuitBoardBlock.getAngleX(state), Direction.Axis.X);
        pos = VecHelper.rotateCentered(pos, CircuitBoardBlock.getAngleY(state), Direction.Axis.Y);
        return pos.add(this.pos.getX(), this.pos.getY(), this.pos.getZ());
    }

    public <T> T data() {
        return (T) customData;
    }

    public <T> Optional<T> data(Class<T> type) {
        if(type.isInstance(customData)) {
            return Optional.of((T) customData);
        }
        return Optional.empty();
    }
}
