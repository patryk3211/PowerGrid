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

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlockEntity;

import static org.patryk3211.powergrid.electricity.base.ThermalBehaviour.BASE_TEMPERATURE;
import static org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock.POWER;

public abstract class LightBulbState {
    protected final Item item;
    protected final ILightBulb bulb;
    protected final LightFixtureBlockEntity fixture;

    protected final float thermalMass;
    protected final float dissipationFactor;
    protected final float overheatTemperature;
    protected float temperature;
    protected boolean burned;
    private int overheatTicks;

    public <T extends Item&ILightBulb> LightBulbState(T bulb, LightFixtureBlockEntity fixture) {
        this.item = bulb;
        this.bulb = bulb;
        this.fixture = fixture;

        var properties = bulb.thermalProperties();
        thermalMass = properties.thermalMass();
        dissipationFactor = properties.dissipationFactor();
        overheatTemperature = properties.overheatTemperature();

        this.burned = false;
    }

    protected void applyPower(float power) {
        if(burned)
            return;
        var energy = power / 20f;
        temperature += energy / thermalMass;
        if(energy < 0 && temperature < BASE_TEMPERATURE)
            temperature = BASE_TEMPERATURE;
    }

    protected void updatePowerLevel(int newLevel) {
        var world = fixture.getLevel();
        var state = fixture.getBlockState();
        if(newLevel != state.getValue(POWER)) {
            world.setBlockAndUpdate(fixture.getBlockPos(), state.setValue(POWER, newLevel));
        }
    }

    public int getPowerLevel() {
        return fixture.getBlockState().getValue(POWER);
    }

    public void tick() {
        if(burned)
            return;

        var filament = fixture.getFilament();
        float dissipatedPower = dissipationFactor * (temperature - BASE_TEMPERATURE);
        applyPower(filament.power() - dissipatedPower);
        filament.setResistance(bulb.resistanceFunction(temperature));

        var world = fixture.getLevel();
        if(isOverheated() && overheatTicks++ >= 4) {
            burned = true;
            filament.setState(false);
            if(world.isClientSide) {
                var pos = fixture.getBlockPos().getCenter();
                world.addParticle(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 0, 0, 0);
            }
            updatePowerLevel(0);
            return;
        } else {
            overheatTicks = 0;
        }

        if(!world.isClientSide) {
            int powerLevel = 0;
            if(temperature > 1400f) {
                powerLevel = 2;
            } else if(temperature > 1200f) {
                powerLevel = 1;
            }
            updatePowerLevel(powerLevel);
        }
    }

    public boolean isOverheated() {
        return temperature >= overheatTemperature;
    }

    public boolean isBurned() {
        return burned;
    }

    public float resistance() {
        return bulb.resistanceFunction(temperature);
    }

    public ItemStack toStack() {
        return new ItemStack(item);
    }

    public boolean isOf(Item item) {
        return this.item == item;
    }

    @Environment(EnvType.CLIENT)
    public abstract PartialModel getModel();
    @NotNull
    @Environment(EnvType.CLIENT)
    public abstract PartialModel getLightModel();

    public float getAlpha() {
        var x = Mth.clamp((temperature - 600f) / (1400f - 600f), 0, 1);
        return x * x;
    }

    public void write(CompoundTag nbt) {
        nbt.putString("Bulb", BuiltInRegistries.ITEM.getKey(item).toString());
        nbt.putFloat("Temperature", temperature);
        if(burned)
            nbt.putBoolean("Burned", true);
    }

    public void read(CompoundTag nbt) {
        var bulbItem = BuiltInRegistries.ITEM.get(new ResourceLocation(nbt.getString("Bulb")));
        if(bulbItem != item) {
            PowerGrid.LOGGER.error("Bulb item validation failed");
            return;
        }
        temperature = nbt.getFloat("Temperature");
        burned = nbt.getBoolean("Burned");
    }

    public static Item getBulbItem(CompoundTag nbt) {
        if(!nbt.contains("Bulb"))
            return null;
        var bulbItem = BuiltInRegistries.ITEM.get(new ResourceLocation(nbt.getString("Bulb")));
        if(!(bulbItem instanceof ILightBulb)) {
            PowerGrid.LOGGER.error("Tried to use a non light bulb item for light bulb state");
            return null;
        }
        return bulbItem;
    }

    public Item getItem() {
        return item;
    }
}
