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

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.EnumProperty;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.components.properties.PropertyEntry;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.INode;

import java.util.ArrayList;
import java.util.List;
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

    private Supplier<World> worldSupplier;
    private BlockPos pos;

    public final List<INode> nodes = new ArrayList<>();
    public final List<AbstractElectricWire> wires = new ArrayList<>();

    public PlacedComponent(NbtCompound tag) {
        this(get(tag.getString("Id")), tag.getInt("X"), tag.getInt("Y"), tag.getUuid("UUID"));
        var propertyMap = tag.getCompound("Properties");
        for(var entry : properties) {
            entry.read(propertyMap);
        }
    }

    public PlacedComponent(PlacedComponent placed) {
        this(placed.component, placed.x, placed.y, placed.uuid);
        this.worldSupplier = placed.worldSupplier;
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
        return ComponentRegistry.REGISTRY.get(new Identifier(id));
    }

    public PlacedComponent(Component component, int x, int y, UUID uuid) {
        this.component = component;
        this.x = x;
        this.y = y;
        this.uuid = uuid;
        for(var property : component.getProperties()) {
            properties.add(PropertyEntry.makeFor(property, this));
        }
    }

    public void withWorld(Supplier<World> worldSupplier, BlockPos pos) {
        this.worldSupplier = worldSupplier;
        this.pos = pos;
    }

    public World getWorld() {
        return worldSupplier.get();
    }

    public void onClientWorld(Supplier<Consumer<World>> callback) {
        var world = getWorld();
        if(world.isClient)
            callback.get().accept(world);
    }

    public void onServerWorld(Supplier<Consumer<World>> callback) {
        var world = getWorld();
        if(!world.isClient)
            callback.get().accept(world);
    }

    public NbtCompound serializeNbt() {
        var tag = new NbtCompound();

        var id = ComponentRegistry.REGISTRY.getId(component);
        tag.putString("Id", id.toString());
        tag.putInt("X", x);
        tag.putInt("Y", y);
        tag.putUuid("UUID", uuid);

        if(!properties.isEmpty()) {
            var propertyMap = new NbtCompound();
            for(var entry : properties) {
                entry.write(propertyMap);
            }
            tag.put("Properties", propertyMap);
        }

        return tag;
    }

    public ComponentFootprint footprint() {
        return component.footprint(this);
    }

    public boolean tick() {
        return component.tick(this);
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
}
