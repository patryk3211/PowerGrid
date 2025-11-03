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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
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
    @Nullable
    private final Runnable overheatCallback;

    private float temperature = 22f;
    private int overheatTicks;
    private Vec3 position;

    public ThermalUnit(UUID componentUUID, int index, float thermalMass, float dissipationFactor, float overheatTemperature, Collection<AbstractElectricWire> heatSources, @Nullable Consumer<Float> temperatureCallback, @Nullable Runnable overheatCallback) {
        this.componentUUID = componentUUID;
        this.index = index;
        this.thermalMass = thermalMass;
        this.dissipationFactor = dissipationFactor;
        this.overheatTemperature = overheatTemperature;
        this.heatSources = heatSources;
        this.temperatureCallback = temperatureCallback;
        this.overheatCallback = overheatCallback;
    }

    private void temperatureChanged() {
        // Heat sources are removed if device has overheated
        if(hasOverheated()) {
            heatSources.forEach(AbstractElectricWire::remove);
            if(overheatCallback != null)
                overheatCallback.run();
        }
        if(temperatureCallback != null)
            temperatureCallback.accept(temperature);
    }

    public boolean hasOverheated() {
        return temperature >= overheatTemperature && overheatTicks >= ThermalBehaviour.OVERHEAT_TICKS;
    }

    public void tick(float dissipationMultiplier) {
        if(hasOverheated())
            return;
        float power = -dissipationFactor * (temperature - BASE_TEMPERATURE) * dissipationMultiplier;
        for(var source : heatSources) {
            if(!source.isConverged())
                return;
            power += source.power();
        }
        temperature += power / 20f / thermalMass;
        if(!Float.isFinite(temperature))
            temperature = BASE_TEMPERATURE;
        if(temperature > overheatTemperature + 10)
            temperature = overheatTemperature + 10;
        if(power < 0 && temperature < 22f)
            temperature = 22f;
        if(temperature >= overheatTemperature && power > 0) {
            ++overheatTicks;
        } else if(power < 0) {
            overheatTicks = 0;
        }
        temperatureChanged();
    }

    public ThermalUnit withPosition(Vec3 position) {
        this.position = position;
        return this;
    }

    public Vec3 getPosition() {
        return position;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getOverheatTemperature() {
        return overheatTemperature;
    }

    public String getKey() {
        return componentUUID.toString() + "-" + index;
    }

    public void read(CompoundTag nbt) {
        temperature = nbt.getFloat(getKey());
        temperatureChanged();
    }

    public void write(CompoundTag nbt) {
        nbt.putFloat(getKey(), temperature);
    }

    public UUID getId() {
        return componentUUID;
    }
}
