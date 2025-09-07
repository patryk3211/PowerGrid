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
package org.patryk3211.powergrid.circuits.components;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.ConstantProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.utility.Unit;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class Component {
    private static final Map<Item, Component> COMPONENT_MAP = new HashMap<>();
    public static final float BASE_Y = 2 / 16f;

    private Supplier<? extends Item> item;
    private final ComponentFootprint footprint;
    private final ImmutableList<ComponentProperty<?>> properties;

    public Component(ComponentFootprint footprint) {
        this.footprint = footprint;

        var properties = new ImmutableList.Builder<ComponentProperty<?>>();
        addProperties(properties);
        this.properties = properties.build();
    }

    static ConstantProperty c(String name, net.minecraft.network.chat.Component value) {
        return new ConstantProperty(PowerGrid.MOD_ID, name, value);
    }

    public static ConstantProperty power(float value) {
        return new ConstantProperty(PowerGrid.MOD_ID, "power", Unit.POWER.format(value));
    }

    @Environment(EnvType.CLIENT)
    public static void modelChanged(BlockPos pos) {
        var renderer = Minecraft.getInstance().levelRenderer;
        renderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {

    }

    public float getPadResistance(int padIndex) {
        return 0.002f;
    }

    public boolean emitExternalTerminals() {
        return false;
    }

    public List<TerminalBoundingBox> terminals(@NotNull PlacedComponent placed) {
        return List.of();
    }

    public abstract void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals);

    /**
     * Called every tick as long as the returned value is true
     * @param placed Placed component state
     * @return Continue ticking
     */
    public boolean tick(@NotNull PlacedComponent placed) {
        return false;
    }

    /**
     * Called when state is updated externally
     * @param placed Placed component state
     */
    public void stateUpdated(@NotNull PlacedComponent placed) {

    }

    void setItem(Supplier<? extends Item> item) {
        this.item = item;
    }

    public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
        return footprint;
    }

    public Item getRequiredItem() {
        return item.get();
    }

    public ImmutableList<ComponentProperty<?>> getProperties() {
        return properties;
    }

    public boolean canPlace(@NotNull PlacedComponent placed, int x, int y) {
        return true;
    }

    @NotNull
    @Environment(EnvType.CLIENT)
    public ResourceLocation getModelId(@NotNull PlacedComponent component) {
        return ComponentRegistry.getId(this);
    }

    @NotNull
    @Environment(EnvType.CLIENT)
    public Collection<ResourceLocation> requestedModels() {
        return List.of(ComponentRegistry.getId(this));
    }

    public static Component forItem(Item item) {
        if(COMPONENT_MAP.containsKey(item))
            return COMPONENT_MAP.get(item);
        for(var entry : ComponentRegistry.entries()) {
            if(entry.item.get() == item) {
                COMPONENT_MAP.put(item, entry);
                return entry;
            }
        }
        COMPONENT_MAP.put(item, null);
        return null;
    }
}
