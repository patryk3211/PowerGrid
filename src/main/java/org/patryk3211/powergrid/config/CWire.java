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
package org.patryk3211.powergrid.config;

import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.wire.WireItem;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

public class CWire extends ConfigBase implements WireValues.Provider {
    private static final int VERSION = 1;

    private static final Object2DoubleMap<ResourceLocation> DEFAULT_VALUES = new Object2DoubleOpenHashMap<>();

    protected final Map<ResourceLocation, ForgeConfigSpec.ConfigValue<Double>> values = new HashMap<>();

    @Override
    public void registerAll(ForgeConfigSpec.Builder builder) {
        builder.comment(".")
                .push("wire");
        DEFAULT_VALUES.forEach((id, value) -> this.values.put(id, builder.define(id.getPath(), value)));
        builder.pop();
    }

    @Override
    public String getName() {
        return "wires-v" + VERSION;
    }

    @Nullable
    public DoubleSupplier get(WireItem item, String suffix) {
        var id = BuiltInRegistries.ITEM.getKey(item).withSuffix("." + suffix);
        var entry = values.get(id);
        return entry == null ? null : entry::get;
    }

    @Override
    public @Nullable DoubleSupplier resistance(WireItem wire) {
        return get(wire, "resistance");
    }

    @Override
    public @Nullable DoubleSupplier maxLength(WireItem wire) {
        return get(wire, "max-length");
    }

    @Override
    public @Nullable DoubleSupplier itemUseMultiplier(WireItem wire) {
        return get(wire, "item-use-multiplier");
    }

    @Override
    public @Nullable DoubleSupplier thermalMass(WireItem wire) {
        return get(wire, "thermal-mass");
    }

    @Override
    public @Nullable DoubleSupplier dissipationFactor(WireItem wire) {
        var pR = get(wire, "resistance");
        var pI = get(wire, "max-current");
        if(pR == null || pI == null)
            return null;
        return () -> {
            var R = pR.getAsDouble();
            var I = pI.getAsDouble();
            var P = R * I * I;
            return P / 150;
        };
    }

    private static void add(ItemBuilder<?, ?> builder, String suffix, double value) {
        var id = PowerGrid.asResource(builder.getName()).withSuffix("." + suffix);
        DEFAULT_VALUES.put(id, value);
    }

    public static <I extends WireItem, P> NonNullUnaryOperator<ItemBuilder<I, P>> set(double resistance, double maxLength, double itemUseMultiplier, double thermalMass, double maxCurrent) {
        return builder -> {
            assertFromPowerGrid(builder);
            add(builder, "resistance", resistance);
            add(builder, "max-length", maxLength);
            add(builder, "item-use-multiplier", itemUseMultiplier);
            add(builder, "thermal-mass", thermalMass);
            add(builder, "max-current", maxCurrent);
            return builder;
        };
    }

    private static void assertFromPowerGrid(ItemBuilder<?, ?> builder) {
        if (!builder.getOwner().getModid().equals(PowerGrid.MOD_ID)) {
            throw new IllegalStateException("Non-Power Grid blocks cannot be added to Power Grid's config.");
        }
    }
}
