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
package org.patryk3211.powergrid.electricity.wire.powercord;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.wire.WireItem;

public class StringLightCordEntity extends CordEntity {
    protected static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(StringLightCordEntity.class, EntityDataSerializers.FLOAT);
    private static final float DISSIPATION_FACTOR = 3.5f / 1800;
    private static final float THERMAL_MASS = 0.005f;

    private ElectricWire pWire1;
    private ElectricWire pWire2;
    private float filamentTemperature;

    // Client rendering stuff
    private int renderIndex;
    public float prevPower, power;

    public static StringLightCordEntity create(Level world, ICordEndpoint endpoint1, ICordEndpoint endpoint2, ItemStack item, @Nullable Float resistance) {
        if(!(item.getItem() instanceof CordItem))
            throw new IllegalArgumentException("ItemStack must be of a CordItem");
        var entity = new StringLightCordEntity(ModdedEntities.STRING_LIGHT_CORD.get(), world);
        entity.setItem((WireItem) item.getItem(), item.getCount());

        entity.resistanceOverride = resistance;

        entity.setEndpoint1(endpoint1);
        entity.setEndpoint2(endpoint2);

        entity.refreshTerminalPositions();
        entity.setXRot(0);
        entity.setOldPosAndRot();
        entity.reapplyPosition();
        return entity;
    }

    public StringLightCordEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(POWER, 0f);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("Filament", filamentTemperature);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        filamentTemperature = nbt.getFloat("Filament");
    }

    @Override
    public int getColor() {
        return 0xff413c31;
    }

    @Override
    public void dropWire() {
        super.dropWire();
        if(pWire1 != null) {
            pWire1.remove();
            pWire1 = null;
        }
        if(pWire2 != null) {
            pWire2.remove();
            pWire2 = null;
        }
    }

    @Override
    public void makeWire() {
        // Cannot make a wire unless both endpoints are valid.
        if(endpoint1 == null || endpoint2 == null)
            return;

        var world = level();
        if(!endpoint1.isValid(world) || !endpoint2.isValid(world))
            return;

        var cordEnd1 = (ICordEndpoint) endpoint1;
        var cordEnd2 = (ICordEndpoint) endpoint2;
        try {
            wire1 = GlobalElectricNetworks.makeConnection(world, cordEnd1.getEndpoint1(), cordEnd2.getEndpoint1(), this, new WorldNetworks.ComplexId(getUUID(), 0));
            wire2 = GlobalElectricNetworks.makeConnection(world, cordEnd1.getEndpoint2(), cordEnd2.getEndpoint2(), this, new WorldNetworks.ComplexId(getUUID(), 1));

            var parallelResistance = (576 * 8) / Math.max(itemCount, 1);
            pWire1 = GlobalElectricNetworks.makeSimpleConnection(world, cordEnd1.getEndpoint1(), cordEnd1.getEndpoint2(), parallelResistance * 2);
            pWire2 = GlobalElectricNetworks.makeSimpleConnection(world, cordEnd2.getEndpoint1(), cordEnd2.getEndpoint2(), parallelResistance * 2);
        } catch(RuntimeException e) {
            PowerGrid.LOGGER.error("Failed to create wire for entity", e);
            kill();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(!level().isClientSide) {
            var power = (pWire1.power() + pWire2.power()) / Math.max(itemCount, 1);
            filamentTemperature += (power - DISSIPATION_FACTOR * filamentTemperature) * 0.05f / THERMAL_MASS;
            var x = Mth.clamp((filamentTemperature - 600f) / (1400f - 600f), 0, 1);
            entityData.set(POWER, x * x);
        } else {
            prevPower = power;
            power = entityData.get(POWER);
        }
    }

    @Override
    protected void unloaded() {
        super.unloaded();
        if(pWire1 != null) {
            pWire1.remove();
            pWire1 = null;
        }
        if(pWire2 != null) {
            pWire2.remove();
            pWire2 = null;
        }
    }

    public void beginRender() {
        renderIndex = 0;
    }

    public int nextColor() {
        var color = switch(renderIndex % 3) {
            case 0 -> 0xFF0000;
            case 1 -> 0x00FF00;
            case 2 -> 0x0000FF;
            default -> -1;
        };
        ++renderIndex;
        return color;
    }
}
