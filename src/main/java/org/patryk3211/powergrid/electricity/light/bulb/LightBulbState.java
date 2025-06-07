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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlockEntity;

import static org.patryk3211.powergrid.electricity.base.ThermalBehaviour.BASE_TEMPERATURE;
import static org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock.POWER;

public abstract class LightBulbState {
    protected final Item item;
    protected final ILightBulb bulb;
    protected final LightFixtureBlockEntity fixture;

    protected final float thermalMass = 0.005f;
    protected final float dissipationFactor;
    protected float temperature;
    protected boolean burned;

    public <T extends Item&ILightBulb> LightBulbState(T bulb, LightFixtureBlockEntity fixture) {
        this.item = bulb;
        this.bulb = bulb;
        this.fixture = fixture;
        this.dissipationFactor = bulb.dissipationFactor();
        this.burned = false;
    }

    protected void applyPower(float power) {
        if(burned)
            return;
        var energy = power / 20f;
        temperature += energy / thermalMass;
    }

    protected void updatePowerLevel(int newLevel) {
        var world = fixture.getWorld();
        var state = fixture.getCachedState();
        if(newLevel != state.get(POWER)) {
            world.setBlockState(fixture.getPos(), state.with(POWER, newLevel));
        }
    }

    public void tick() {
        if(burned)
            return;

        var filament = fixture.getFilament();
        float dissipatedPower = dissipationFactor * (temperature - BASE_TEMPERATURE);
        applyPower(filament.power() - dissipatedPower);
        filament.setResistance(bulb.resistanceFunction(temperature));

        if(isOverheated()) {
            burned = true;
            filament.setState(false);
            return;
        }

        var world = fixture.getWorld();
        if(!world.isClient) {
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
        return temperature >= 1700f;
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

    public void write(NbtCompound nbt) {
        nbt.putString("Bulb", Registries.ITEM.getId(item).toString());
        nbt.putFloat("Temperature", temperature);
        if(burned)
            nbt.putBoolean("Burned", true);
    }

    public void read(NbtCompound nbt) {
        var bulbItem = Registries.ITEM.get(new Identifier(nbt.getString("Bulb")));
        if(!(bulbItem instanceof ILightBulb)) {
            PowerGrid.LOGGER.error("Tried to use a non light bulb item for light bulb state");
            return;
        }
        temperature = nbt.getFloat("Temperature");
        burned = nbt.getBoolean("Burned");
    }

    public static Item getBulbItem(NbtCompound nbt) {
        var bulbItem = Registries.ITEM.get(new Identifier(nbt.getString("Bulb")));
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
