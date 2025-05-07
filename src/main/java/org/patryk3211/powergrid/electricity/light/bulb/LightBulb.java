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
package org.patryk3211.powergrid.electricity.light.bulb;

import com.jozufozu.flywheel.core.PartialModel;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.fabric.EnvExecutor;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.fabricmc.api.EnvType;
import net.minecraft.item.Item;

import java.util.function.Function;
import java.util.function.Supplier;

public class LightBulb extends Item implements ILightBulb {
    Supplier<Function<State, PartialModel>> modelSupplier;

    float k = 0.005f;
    float T_mid = 1200;
    float R_max = 100;
    float R_min = 15;
    float dissipationFactor = 0.1f;

    public LightBulb(Settings settings) {
        super(settings);
    }

    public static <I extends LightBulb, P> NonNullUnaryOperator<ItemBuilder<I, P>> setModelProvider(Supplier<Function<State, PartialModel>> provider) {
        return b -> {
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> b.onRegister(item -> item.modelSupplier = provider));
            return b;
        };
    }

    public static <I extends LightBulb, P> NonNullUnaryOperator<ItemBuilder<I, P>> setProperties(float minResistance, float maxResistance, float curveConstant, float midpointTemperature, float dissipationFactor) {
        return b -> {
            b.onRegister(item -> {
                item.k = curveConstant;
                item.T_mid = midpointTemperature;
                item.R_max = maxResistance;
                item.R_min = minResistance;
            });
            return b;
        };
    }

    @Override
    public Supplier<Function<State, PartialModel>> getModelProvider() {
        return modelSupplier;
    }

    @Override
    public float resistanceFunction(float temperature) {
        return (float) (R_min + (R_max - R_min) / (1 + Math.exp(-k * (temperature - T_mid))));
    }

    @Override
    public float dissipationFactor() {
        return dissipationFactor;
    }
}
