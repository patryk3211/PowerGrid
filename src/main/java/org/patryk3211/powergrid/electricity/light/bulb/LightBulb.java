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
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlockEntity;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class LightBulb extends Item implements ILightBulb, IHaveElectricProperties {
    protected Supplier<Function<State, PartialModel>> modelSupplier = null;

    protected float k = 0.005f;
    protected float T_mid = 1200;
    protected float R_max = 100;
    protected float R_min = 15;
    protected Properties thermalProperties;

    protected float power = 0;
    protected float voltage = 0;

    public LightBulb(Settings settings) {
        super(settings);
    }

    public static <I extends LightBulb, P> NonNullUnaryOperator<ItemBuilder<I, P>> setModelProvider(Supplier<Function<State, PartialModel>> provider) {
        return b -> {
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> b.onRegister(item -> item.modelSupplier = provider));
            return b;
        };
    }

    public static <I extends LightBulb, P> NonNullUnaryOperator<ItemBuilder<I, P>> setProperties(float minResistance, float maxResistance, float curveConstant, float midpointTemperature, float dissipationFactor, float overheatTemperature, float thermalMass) {
        return b -> {
            b.onRegister(item -> {
                item.k = curveConstant;
                item.T_mid = midpointTemperature;
                item.R_max = maxResistance;
                item.R_min = minResistance;
                item.thermalProperties = new Properties(dissipationFactor, thermalMass, overheatTemperature);
            });
            return b;
        };
    }

    public static <I extends LightBulb, P> NonNullUnaryOperator<ItemBuilder<I, P>> setProperties(float ratedPower, float ratedVoltage, float minResistance, float operatingTemperature, float thermalMass) {
        float R_max = ratedVoltage * ratedVoltage / ratedPower;
        final float T_mid = 750f;
        final float k = 0.005f;
        final float dissipationFactor = ratedPower / (operatingTemperature - ThermalBehaviour.BASE_TEMPERATURE);
        NonNullUnaryOperator<ItemBuilder<I, P>> result = setProperties(minResistance, R_max, k, T_mid, dissipationFactor, operatingTemperature + 250f, thermalMass);
        return result.andThen(b -> {
            b.onRegister(item -> {
                item.power = ratedPower;
                item.voltage = ratedVoltage;
            });
            return b;
        });
    }

    @Override
    public float resistanceFunction(float temperature) {
        return (float) (R_min + (R_max - R_min) / (1 + Math.exp(-k * (temperature - T_mid))));
    }

    @Override
    public Properties thermalProperties() {
        return thermalProperties;
    }

    @Override
    public LightBulbState createState(LightFixtureBlockEntity fixture) {
        return new SimpleState(this, fixture, modelSupplier);
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        if(voltage > 0 && power > 0) {
            Voltage.rated(voltage, player, tooltip);
            Power.rated(power, player, tooltip);
        } else {
            Resistance.series(R_max, player, tooltip);
        }
    }

    public enum State {
        OFF, LOW_POWER, ON, BROKEN
    }

    public static class SimpleState extends LightBulbState {
        @Environment(EnvType.CLIENT)
        public Function<State, PartialModel> modelProvider;

        public <T extends Item & ILightBulb> SimpleState(T bulb, LightFixtureBlockEntity fixture, Supplier<Function<State, PartialModel>> modelProviderSupplier) {
            super(bulb, fixture);
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> modelProvider = modelProviderSupplier.get());
        }

        @Override
        @Environment(EnvType.CLIENT)
        public PartialModel getModel() {
            var state = State.OFF;
            if(burned) {
                state = State.BROKEN;
            } else {
                var blockState = fixture.getCachedState();
                var powerLevel = blockState.get(LightFixtureBlock.POWER);
                if(powerLevel == 1) {
                    state = State.LOW_POWER;
                } else if(powerLevel == 2) {
                    state = State.ON;
                }
            }
            return modelProvider.apply(state);
        }
    }
}
