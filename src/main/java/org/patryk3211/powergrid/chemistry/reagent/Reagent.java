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
import org.jetbrains.annotations.Nullable;

public class Reagent {
    public final Properties properties;
    private Item item;
    private Fluid fluid;

    public Reagent(Properties properties) {
        this.properties = properties;
    }

    public Reagent withItem(Item item) {
        var copy = new Reagent(properties);
        copy.item = item;
        copy.fluid = fluid;
        return copy;
    }

    public Reagent withFluid(Fluid fluid) {
        var copy = new Reagent(properties);
        copy.item = item;
        copy.fluid = fluid;
        return copy;
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

    public static class Properties {
        public static final Properties EMPTY = new Properties();
        public static final Properties WATER = new Properties().meltingPoint(0).boilingPoint(100).heatCapacity(75.38f);
        public static final Properties OXYGEN = new Properties().meltingPoint(-218.8f).boilingPoint(-182.9f).heatCapacity(29.37f);
        public static final Properties HYDROGEN = new Properties().meltingPoint(-259.2f).boilingPoint(-252.8f).heatCapacity(28.84f);
        public static final Properties NITROGEN = new Properties().meltingPoint(-209.8f).boilingPoint(-195.7f).heatCapacity(29.12f);

        public static final Properties SULFUR = new Properties().meltingPoint(115.2f).boilingPoint(444.6f).heatCapacity(22.75f);
        public static final Properties SULFUR_DIOXIDE = new Properties().meltingPoint(-72.0f).boilingPoint(10.0f).heatCapacity(42.5f);

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
