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
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.ViaComponent;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.*;

import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_SIZE;
import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_TO_GRID_SCALE;

public class CircuitSchematic {
    private final CircuitLayer front = new CircuitLayer();
    private final CircuitLayer back = new CircuitLayer();

    // This layer is generated from components.
    private final CircuitLayer pads = new CircuitLayer();

    private final List<PlacedComponent> components = new ArrayList<>();

    @Nullable
    private String name;

    public CircuitSchematic() {

    }

    public CircuitSchematic(CircuitSchematic schematic) {
        this.name = schematic.name;
        front.from(schematic.front);
        back.from(schematic.back);
        for(var component : schematic.components) {
            var placed = new PlacedComponent(component);
            components.add(placed);
            addPads(placed);
        }
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public static CircuitSchematic fromNbt(NbtCompound nbt) {
        if(nbt == null)
            return null;
        var schematic = new CircuitSchematic();
        schematic.deserializeNbt(nbt);
        return schematic;
    }

    public static CircuitSchematic fromStack(ItemStack stack) {
        if(stack.isEmpty() || !stack.hasNbt())
            return null;
        return fromNbt(stack.getNbt().getCompound("Schematic"));
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
        if(name != null) {
            tag.putString("Name", name);
        }
        return tag;
    }

    public void deserializeNbt(NbtCompound tag) {
        try {
            front.deserialize(tag.getLongArray("Front"));
            back.deserialize(tag.getLongArray("Back"));

            if (tag.contains("Name")) {
                name = tag.getString("Name");
            } else {
                name = null;
            }

            components.clear();
            var list = tag.getList("Components", NbtElement.COMPOUND_TYPE);
            for (var element : list) {
                components.add(new PlacedComponent((NbtCompound) element));
            }
            rebuildPads();
        } catch(RuntimeException e) {
            clear();
            PowerGrid.LOGGER.error("Failed to deserialize circuit schematic NBT", e);
        }
    }

    public void rebuildPads() {
        pads.clear();
        for(var placed : components) {
            addPads(placed);
        }
    }

    private void addPads(PlacedComponent placed) {
        var componentPads = placed.footprint().getPads();
        for(var point : componentPads.keySet()) {
            pads.set(placed.x * GRID_TO_GRID_SCALE + point.x(), placed.y * GRID_TO_GRID_SCALE + point.y());
        }
    }

    public void placeComponent(PlacedComponent basePlacedState, int x, int y) {
        var placed = new PlacedComponent(basePlacedState, x, y);
        components.add(placed);
        addPads(placed);
    }

    public void removeComponents(int x, int y, int width, int height) {
        components.removeIf(placed -> placed.intersects32(x, y, width, height));
        rebuildPads();
    }

    @Nullable
    public PlacedComponent getComponent(int x, int y) {
        for(var placed : components) {
            if(x < placed.x || y < placed.y)
                continue;
            var footprint = placed.footprint();
            if(x >= placed.x + footprint.getWidth() || y >= placed.y + footprint.getHeight())
                continue;
            return placed;
        }
        return null;
    }

    public boolean isPad(int x, int y) {
        return pads.get(x, y);
    }

    public CircuitLayer getLayer(Layer layer) {
        return layer == Layer.FRONT ? front : back;
    }

    public boolean getLayer(Layer layer, int x, int y) {
        return getLayer(layer).get(x, y);
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
        var tag = new NbtCompound();
        tag.put("Schematic", serializeNbt());
        stack.setNbt(tag);
        if(name != null)
            stack.setCustomName(Text.literal(name));
        return stack;
    }

    private boolean getAreaState(CircuitLayer layer, int x, int y) {
        return layer.get(x, y) && !isPad(x, y);
    }

    public List<Area> calculateAreas(Layer forLayer) {
        var areas = new ArrayList<Area>();
        var layer = getLayer(forLayer);

        var visitMap = new BitSet(GRID_SIZE * GRID_SIZE);
        for(int x = 0; x < GRID_SIZE; ++x) {
            for(int y = 0; y < GRID_SIZE; ++y) {
                var bit = getAreaState(layer, x, y);
                if(!bit || visitMap.get(x + y * GRID_SIZE))
                    continue;
                int x1;
                for(x1 = x + 1; x1 < GRID_SIZE; ++x1) {
                    if(!getAreaState(layer, x1, y) || visitMap.get(x1 + y * GRID_SIZE))
                        break;
                }
                int y1;
                for(y1 = y + 1; y1 < GRID_SIZE; ++y1) {
                    if(!getAreaState(layer, x, y1) || visitMap.get(x + y1 * GRID_SIZE))
                        break;
                    boolean isFull = true;
                    for(int i = x; i < x1; ++i) {
                        if(!getAreaState(layer, i, y1) || visitMap.get(i + y1 * GRID_SIZE)) {
                            isFull = false;
                            break;
                        }
                    }
                    if(!isFull)
                        break;
                }

                for(int x2 = x; x2 < x1; ++x2) {
                    for(int y2 = y; y2 < y1; ++y2) {
                        visitMap.set(x2 + y2 * GRID_SIZE);
                    }
                }
                areas.add(new Area(x, y, x1, y1));
            }
        }

        return areas;
    }

    public boolean canPlace(PlacedComponent component, int x, int y) {
        if(x < 0 || y < 0)
            return false;
        var width = component.footprint().getWidth();
        var height = component.footprint().getHeight();
        if(x + width > 16 || y + height > 16)
            return false;

        if(!component.canPlace(x, y))
            return false;

        // Check pad clearance
        var thisFootprint = component.footprint();
        for(var pad : thisFootprint.getPads().keySet()) {
            if(pads.get(x * GRID_TO_GRID_SCALE + pad.x(), y * GRID_TO_GRID_SCALE + pad.y()))
                return false;
        }

        boolean thisVia = component.component instanceof ViaComponent;
        for(var placed : components) {
            boolean thatVia = placed.component instanceof ViaComponent;
            if(thisVia != thatVia) {
                // Vias only collide with other vias, components don't collide with vias
                continue;
            }
            if(placed.intersects(x, y, width, height))
                return false;
        }
        return true;
    }

    private Optional<Node> makeComponentNode(int x, int y) {
        var placed = getComponent(x / GRID_TO_GRID_SCALE, y / GRID_TO_GRID_SCALE);
        if(placed == null)
            return Optional.empty();
        var footprint = placed.footprint();
        int lX = x - placed.x * GRID_TO_GRID_SCALE;
        int lY = y - placed.y * GRID_TO_GRID_SCALE;
        var padData = footprint.getPads().get(new Point(lX, lY));
        if(padData == null || padData.nodeIndex() < 0)
            return Optional.empty();
        return Optional.of(new Node(placed, padData.nodeIndex()));
    }

    public int getId(@NotNull PlacedComponent placed) {
        for(int i = 0; i < components.size(); ++i) {
            if(components.get(i) == placed)
                return i;
        }
        return -1;
    }

    private boolean shouldVisit(Layer layer, int x, int y, VisitMap visitMap) {
        if(x < 0 || y < 0 || x >= GRID_SIZE || y >= GRID_SIZE)
            return false;
        if(!visitMap.canVisit(layer, x, y))
            return false;
        return getLayer(layer, x, y) || isPad(x, y);
    }

    @NotNull
    private Collection<Node> flood(Layer layer, int x, int y, VisitMap visitMap) {
        if(!visitMap.canVisit(layer, x, y))
            return List.of();

        var toVisit = new ArrayList<Point>();
        toVisit.add(new Point(x, y));

        var nodes = new HashSet<Node>();
        while(!toVisit.isEmpty()) {
            var node = toVisit.remove(0);
            int nX = node.x();
            int nY = node.y();

            if(visitMap.visit(layer, nX, nY)) {
                if(isPad(nX, nY)) {
                    // We need to trace the other layer and check for component node.
                    var componentNode = makeComponentNode(nX, nY);
                    componentNode.ifPresent(nodes::add);

                    // Flood opposite layer.
                    var componentNodes = flood(layer.opposite(), nX, nY, visitMap);
                    nodes.addAll(componentNodes);
                }

                // Add neighbors that haven't been visited.
                if(shouldVisit(layer, nX + 1, nY, visitMap))
                    toVisit.add(new Point(nX + 1, nY));
                if(shouldVisit(layer, nX - 1, nY, visitMap))
                    toVisit.add(new Point(nX - 1, nY));
                if(shouldVisit(layer, nX, nY + 1, visitMap))
                    toVisit.add(new Point(nX, nY + 1));
                if(shouldVisit(layer, nX, nY - 1, visitMap))
                    toVisit.add(new Point(nX, nY - 1));
            }
        }
        return nodes;
    }

    public Collection<Collection<Node>> findNodeBundles() {
        var visitMap = new VisitMap();
        var bundles = new ArrayList<Collection<Node>>();
        for(var placed : components) {
            for(var pad : placed.footprint().getPads().keySet()) {
                var x = placed.x * GRID_TO_GRID_SCALE + pad.x();
                var y = placed.y * GRID_TO_GRID_SCALE + pad.y();
                var nodes = flood(Layer.FRONT, x, y, visitMap);
                if(nodes.size() >= 2)
                    bundles.add(nodes);
            }
        }
        return bundles;
    }

    public void clear() {
        front.clear();
        back.clear();
        pads.clear();
        components.clear();
    }

    @Nullable
    public String getName() {
        return name;
    }

    public enum Layer {
        FRONT, BACK;

        public Layer opposite() {
            return this == FRONT ? BACK : FRONT;
        }
    }

    public record Node(PlacedComponent placed, int pad) {
        public float getPadResistance() {
            return placed.component.getPadResistance(pad);
        }
    }

    private static class VisitMap {
        private final BitSet front = new BitSet(GRID_SIZE * GRID_SIZE);
        private final BitSet back = new BitSet(GRID_SIZE * GRID_SIZE);

        public boolean visit(Layer layer, int x, int y) {
            var set = layer == Layer.FRONT ? front : back;
            var result = !set.get(x + y * GRID_SIZE);
            set.set(x + y * GRID_SIZE);
            return result;
        }

        public boolean canVisit(Layer layer, int x, int y) {
            var set = layer == Layer.FRONT ? front : back;
            return !set.get(x + y * GRID_SIZE);
        }
    }
}
