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

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

public class CThermal extends ConfigBase implements ThermalValues.Provider {
    private static final int VERSION = 1;

    private static final Object2DoubleMap<ResourceLocation> DEFAULT_THERMAL_MASS = new Object2DoubleOpenHashMap<>();
    private static final Object2DoubleMap<ResourceLocation> DEFAULT_POWERS = new Object2DoubleOpenHashMap<>();

    protected final Map<ResourceLocation, ForgeConfigSpec.ConfigValue<Double>> thermalMasses = new HashMap<>();
    protected final Map<ResourceLocation, ForgeConfigSpec.ConfigValue<Double>> power = new HashMap<>();

    @Override
    public void registerAll(ForgeConfigSpec.Builder builder) {
        builder.comment(".", Comments.thermalMass)
                .push("mass");
        DEFAULT_THERMAL_MASS.forEach((id, value) -> this.thermalMasses.put(id, builder.define(id.getPath(), value)));
        builder.pop();

        builder.comment(".", Comments.watt, Comments.power)
                .push("power");
        DEFAULT_POWERS.forEach((id, value) -> this.power.put(id, builder.define(id.getPath(), value)));
        builder.pop();
    }

    @Override
    public String getName() {
        return "thermal-v" + VERSION;
    }

    @Nullable
    @Override
    public DoubleSupplier getMass(Block block) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        var entry = thermalMasses.get(id);
        return entry == null ? null : entry::get;
    }

    @Nullable
    @Override
    public DoubleSupplier getPower(Block block) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        var entry = power.get(id);
        return entry == null ? null : entry::get;
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> maxPower(double power, double thermalMass) {
        return builder -> {
            assertFromPowerGrid(builder);
            var id = PowerGrid.asResource(builder.getName());
            DEFAULT_THERMAL_MASS.put(id, thermalMass);
            DEFAULT_POWERS.put(id, power);
            return builder;
        };
    }

    private static void assertFromPowerGrid(BlockBuilder<?, ?> builder) {
        if (!builder.getOwner().getModid().equals(PowerGrid.MOD_ID)) {
            throw new IllegalStateException("Non-Power Grid blocks cannot be added to Power Grid's config.");
        }
    }

    private static class Comments {
        static String watt = "[in Watts]";
        static String thermalMass = "Configures the amount of energy needed to raise the temperature of the device (this allows for higher temporary loads)";
        static String power = "Configures the maximum power dissipated by the device";
    }
}
