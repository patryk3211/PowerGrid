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

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class Reagent implements ReagentConvertible {
    public static final int BLOCK_MOLE_AMOUNT = 4000;
    public static final int FLUID_MOLE_AMOUNT = BLOCK_MOLE_AMOUNT;

    public static final double FLUID_MOLE_RATIO = (double) FluidConstants.BLOCK / FLUID_MOLE_AMOUNT;

    private static final Map<Fluid, Reagent> FLUID_MAP = new HashMap<>();
    private static final Map<Item, Reagent> ITEM_MAP = new HashMap<>();

    private RegistryEntry<Reagent> registryEntry;
    public final Properties properties;
    private Item item;
    private int itemAmount;
    private float itemTemperature;
    private Fluid fluid;
    private float fluidTemperature;
    private String translationKey;
    private int particleColor = 0;
    private ReagentState fixedState = null;

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

    public Reagent withItem(Item item, int amount, float temperature) {
        this.item = item;
        this.itemAmount = amount;
        this.itemTemperature = temperature;
        ITEM_MAP.put(item, this);
        return this;
    }

    public Reagent withFluid(Fluid fluid, float temperature) {
        this.fluid = fluid;
        this.fluidTemperature = temperature;
        FLUID_MAP.put(fluid, this);
        return this;
    }

    public void withFixedState(ReagentState state) {
        this.fixedState = state;
    }

    public void withParticleColor(int color) {
        this.particleColor = color;
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

    public float getFluidTemperature() {
        return fluidTemperature;
    }

    @Nullable
    public Item asItem() {
        return item;
    }

    public int getItemAmount() {
        return itemAmount;
    }

    public float getItemTemperature() {
        return itemTemperature;
    }

    public int getParticleColor() {
        return particleColor;
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

    public ReagentState getState(float temperature) {
        if(fixedState != null)
            return fixedState;
        return properties.getState(temperature);
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

        protected ReagentState getState(float temperature) {
            if(temperature >= boilingPoint)
                return ReagentState.GAS;
            if(temperature >= meltingPoint)
                return ReagentState.LIQUID;
            return ReagentState.SOLID;
        }

        public float getMeltingPoint() {
            return meltingPoint;
        }

        public float getBoilingPoint() {
            return boilingPoint;
        }
    }
}
