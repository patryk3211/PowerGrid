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
package org.patryk3211.powergrid.circuits.thermal;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

import static org.patryk3211.powergrid.electricity.base.ThermalBehaviour.BASE_TEMPERATURE;

public class ThermalUnit {
    private final UUID componentUUID;
    private final int index;

    private final float thermalMass;
    private final float dissipationFactor;
    private final float overheatTemperature;
    private final Collection<AbstractElectricWire> heatSources;
    @Nullable
    private final Consumer<Float> temperatureCallback;

    private float temperature = 22f;

    public ThermalUnit(UUID componentUUID, int index, float thermalMass, float dissipationFactor, float overheatTemperature, Collection<AbstractElectricWire> heatSources, @Nullable Consumer<Float> temperatureCallback) {
        this.componentUUID = componentUUID;
        this.index = index;
        this.thermalMass = thermalMass;
        this.dissipationFactor = dissipationFactor;
        this.overheatTemperature = overheatTemperature;
        this.heatSources = heatSources;
        this.temperatureCallback = temperatureCallback;
    }

    public void tick() {
        float power = -dissipationFactor * (temperature - BASE_TEMPERATURE);
        for(var source : heatSources) {
            power += source.power();
        }
        temperature += power / 20f / thermalMass;
        if(temperatureCallback != null)
            temperatureCallback.accept(temperature);
    }

    public String getKey() {
        return componentUUID.toString() + "-" + index;
    }

    public void read(NbtCompound nbt) {
        temperature = nbt.getFloat(getKey());
        if(temperatureCallback != null)
            temperatureCallback.accept(temperature);
    }

    public void write(NbtCompound nbt) {
        nbt.putFloat(getKey(), temperature);
    }
}
