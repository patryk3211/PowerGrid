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
package org.patryk3211.powergrid.chemistry.reagent;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class Reagent implements ReagentConvertible {
    public static final double FLUID_MOLE_RATIO = 81000.0 / 4000.0;
    private static final Map<Fluid, Reagent> FLUID_MAP = new HashMap<>();
    private static final Map<Item, Reagent> ITEM_MAP = new HashMap<>();

    private RegistryEntry<Reagent> registryEntry;
    public final Properties properties;
    private Item item;
    private int itemAmount;
    private Fluid fluid;
    private String translationKey;

    public Reagent(Properties properties) {
        this.properties = properties;
    }

    public RegistryEntry<Reagent> getRegistryEntry() {
        if(registryEntry == null)
            registryEntry = ReagentRegistry.REGISTRY.getEntry(this);
        return registryEntry;
    }

    public boolean isIn(TagKey<Reagent> tag) {
        return getRegistryEntry().isIn(tag);
    }

    public Reagent withItem(Item item, int amount) {
        this.item = item;
        this.itemAmount = amount;
        ITEM_MAP.put(item, this);
        return this;
    }

    public Reagent withFluid(Fluid fluid) {
        this.fluid = fluid;
        FLUID_MAP.put(fluid, this);
        return this;
    }

    public float getMeltingPoint() {
        return properties.meltingPoint;
    }

    public float getBoilingPoint() {
        return properties.boilingPoint;
    }

    public float getHeatCapacity() {
        return properties.heatCapacity;
    }

    @Override
    public String toString() {
        return ReagentRegistry.REGISTRY.getId(this).getPath();
    }

    @Nullable
    public Fluid asFluid() {
        return fluid;
    }

    @Nullable
    public Item asItem() {
        return item;
    }

    public int getItemAmount() {
        return itemAmount;
    }

    public static Reagent getReagent(Fluid fluid) {
        return FLUID_MAP.get(fluid);
    }

    public static Reagent getReagent(Item item) {
        return ITEM_MAP.get(item);
    }

    @Override
    public Reagent asReagent() {
        return this;
    }

    protected String getOrCreateTranslationKey() {
        if(this.translationKey == null)
            this.translationKey = Util.createTranslationKey("reagent", ReagentRegistry.REGISTRY.getId(this));
        return this.translationKey;
    }

    public String getTranslationKey() {
        return this.getOrCreateTranslationKey();
    }

    public String getTranslationKey(ReagentStack stack) {
        return this.getOrCreateTranslationKey();
    }

    public static class Properties {
        public static final Properties EMPTY = new Properties();

        // Note: At the moment these are independent of pressure,
        // should this change, I need to remember that the boiling point
        // of water seems to correspond nicely to T = 100 * log_100(p)
        private float meltingPoint;
        private float boilingPoint;
        private float heatCapacity;

        public Properties() {
            meltingPoint = -273.15f;
            boilingPoint = -273.15f;
            heatCapacity = 1;
        }

        public Properties copy() {
            var properties = new Properties();
            properties.meltingPoint = meltingPoint;
            properties.boilingPoint = boilingPoint;
            properties.heatCapacity = heatCapacity;
            return properties;
        }

        public Properties heatCapacity(float heatCapacity) {
            this.heatCapacity = heatCapacity;
            return this;
        }

        public Properties meltingPoint(float temperature) {
            this.meltingPoint = temperature;
            return this;
        }

        public Properties boilingPoint(float temperature) {
            this.boilingPoint = temperature;
            return this;
        }

        public ReagentState getState(float temperature) {
            if(temperature >= boilingPoint)
                return ReagentState.GAS;
            if(temperature >= meltingPoint)
                return ReagentState.LIQUID;
            return ReagentState.SOLID;
        }
    }
}
