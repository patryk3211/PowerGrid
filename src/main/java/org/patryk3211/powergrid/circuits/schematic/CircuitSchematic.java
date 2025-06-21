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

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ViaComponent;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.ArrayList;
import java.util.List;

public class CircuitSchematic {
    private final CircuitLayer front = new CircuitLayer();
    private final CircuitLayer back = new CircuitLayer();

    // This layer is generated from components.
    private final CircuitLayer pads = new CircuitLayer();

    private final List<PlacedComponent> components = new ArrayList<>();

    public CircuitSchematic() {

    }

    public CircuitSchematic(CircuitSchematic schematic) {
        front.from(schematic.front);
        back.from(schematic.back);
        for(var component : schematic.components) {
            components.add(new PlacedComponent(component.component, component.x, component.y));
        }
    }

    public NbtCompound serializeNbt() {
        var tag = new NbtCompound();
        tag.put("Front", front.serializeNbt());
        tag.put("Back", back.serializeNbt());

        var list = new NbtList();
        for(var component : components) {
            list.add(component.serializeNbt());
        }
        tag.put("Components", list);
        return tag;
    }

    public void deserializeNbt(NbtCompound tag) {
        front.deserialize(tag.getLongArray("Front"));
        back.deserialize(tag.getLongArray("Back"));

        components.clear();
        var list = tag.getList("Components", NbtElement.COMPOUND_TYPE);
        for(var element : list) {
            components.add(new PlacedComponent((NbtCompound) element));
        }
        rebuildPads();
    }

    public void rebuildPads() {
        pads.clear();
        for(var placed : components) {
            addPads(placed);
        }
    }

    private void addPads(PlacedComponent placed) {
        var componentPads = placed.component.footprint().getPads();
        for(var point : componentPads.keySet()) {
            pads.set(placed.x * 2 + point.x(), placed.y * 2 + point.y());
        }
    }

    public void placeComponent(Component component, int x, int y) {
        var placed = new PlacedComponent(component, x, y);
        components.add(placed);
        addPads(placed);
    }

    public CircuitLayer front() {
        return front;
    }

    public CircuitLayer back() {
        return back;
    }

    public CircuitLayer pads() {
        return pads;
    }

    public List<PlacedComponent> components() {
        return components;
    }

    public ItemStack toItemStack() {
        var stack = ModdedItems.CIRCUIT_SCHEMATIC.asStack();
        stack.setNbt(serializeNbt());
        return stack;
    }

    public boolean canPlace(Component component, int x, int y) {
        if(x < 0 || y < 0)
            return false;
        var width = component.footprint().getWidth();
        var height = component.footprint().getHeight();
        if(x + width > 16 || y + height > 16)
            return false;

        boolean thisVia = component instanceof ViaComponent;
        for(var placed : components) {
            boolean thatVia = placed.component instanceof ViaComponent;
            if(thisVia != thatVia) {
                // Vias only collide with other vias, components don't collide with vias
                continue;
            }
            var footprint = placed.component.footprint();
            if(Math.abs(x - placed.x) * 2 < (width + footprint.getWidth()) && Math.abs(y - placed.y) * 2 < (height + footprint.getHeight()))
                return false;
        }
        return true;
    }
}
