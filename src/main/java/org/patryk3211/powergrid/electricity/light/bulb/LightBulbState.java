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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlockEntity;

import static org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock.POWER;

public abstract class LightBulbState implements ElectricBehaviour.SyncAppender {
    protected final Item item;
    protected final ILightBulb bulb;
    protected final LightFixtureBlockEntity fixture;

    protected final float thermalMass;
    protected final float dissipationFactor;
    protected final float overheatTemperature;
    protected float temperature;
    protected boolean burned;
    private int overheatTicks;
    private boolean playEffect;

    private Float cachedAmbientTemperature = null;

    @Nullable
    protected DyeColor color;

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

    protected void applyPower(double power) {
        if(burned)
            return;
        double energy = power / 20.0;
        temperature += (float) (energy / thermalMass);
        if(energy < 0 && temperature < cachedAmbientTemperature)
            temperature = cachedAmbientTemperature;
    }

    protected void updatePowerLevel(int newLevel) {
        var world = fixture.getLevel();
        var state = fixture.getBlockState();
        if(newLevel != state.getValue(POWER)) {
            world.setBlock(fixture.getBlockPos(), state.setValue(POWER, newLevel), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    public int getPowerLevel() {
        return fixture.getBlockState().getValue(POWER);
    }

    private void burnEffect() {
        var world = fixture.getLevel();
        if(world.isClientSide) {
            var pos = fixture.getBlockPos().getCenter();
            world.addParticle(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    public void tick() {
        if(burned)
            return;
        var world = fixture.getLevel();
        if(cachedAmbientTemperature == null) {
            cachedAmbientTemperature = ThermalBehaviour.getAmbientTemperature(world, fixture.getBlockPos());
        }

        if(!world.isClientSide) {
            var filament = fixture.getFilament();
            float dissipatedPower = dissipationFactor * (temperature - cachedAmbientTemperature);
            if(filament.isConverged())
                applyPower(filament.power() - dissipatedPower);
            if(!Float.isFinite(temperature))
                temperature = cachedAmbientTemperature;
            filament.setResistance(bulb.resistanceFunction(temperature));

            if (isOverheated() && overheatTicks++ >= 4) {
                burned = true;
                playEffect = true;
                filament.setState(false);
                updatePowerLevel(0);
                fixture.notifyUpdate();
                return;
            } else if (!isOverheated()) {
                overheatTicks = 0;
            }
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
    @Environment(EnvType.CLIENT)
    public abstract PartialModel getDyedBulb();

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
        if(playEffect) {
            nbt.putBoolean("Effect", true);
            playEffect = false;
        }
        if(color != null)
            nbt.putInt("Color", color.ordinal());
    }

    public void read(CompoundTag nbt) {
        var bulbItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(nbt.getString("Bulb")));
        if(bulbItem != item) {
            PowerGrid.LOGGER.error("Bulb item validation failed");
            return;
        }
        temperature = nbt.getFloat("Temperature");
        burned = nbt.getBoolean("Burned");
        fixture.getFilament().setState(!burned);
        if(nbt.getBoolean("Effect")) {
            burnEffect();
        }

        if(bulb.canBeDyed() && nbt.contains("Color")) {
            color = DyeColor.values()[nbt.getInt("Color")];
        } else {
            color = null;
        }
    }

    public static Item getBulbItem(CompoundTag nbt) {
        if(!nbt.contains("Bulb"))
            return null;
        var bulbItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(nbt.getString("Bulb")));
        if(!(bulbItem instanceof ILightBulb)) {
            PowerGrid.LOGGER.error("Tried to use a non light bulb item for light bulb state");
            return null;
        }
        return bulbItem;
    }

    public Item getItem() {
        return item;
    }

    public boolean setColor(DyeColor color) {
        if(bulb.canBeDyed()) {
            this.color = color;
            return true;
        }
        return false;
    }

    public DyeColor getColor() {
        return color;
    }

    @Override
    public void writeToSync(FriendlyByteBuf buffer) {
        buffer.writeFloat(temperature);
    }

    @Override
    public void readFromSync(FriendlyByteBuf buffer) {
        temperature = buffer.readFloat();
    }
}
