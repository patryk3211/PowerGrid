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
package org.patryk3211.powergrid.electricity.light.string;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedDataComponents;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.wire.WireItem;
import org.patryk3211.powergrid.electricity.wire.powercord.CordEntity;
import org.patryk3211.powergrid.electricity.wire.powercord.CordItem;
import org.patryk3211.powergrid.electricity.wire.powercord.ICordEndpoint;

public class StringLightCordEntity extends CordEntity {
    protected static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(StringLightCordEntity.class, EntityDataSerializers.FLOAT);
    private static final float DISSIPATION_FACTOR = 3.5f / 1800;
    private static final float THERMAL_MASS = 0.0005f;

    private ElectricWire pWire1;
    private ElectricWire pWire2;
    private float filamentPower;
    private float filamentTemperature;
    private int lazyTick = 0;
    private boolean state;

    // Client rendering stuff
    private int renderIndex;
    public float prevPower, power;

    private int[] colorPattern;

    public static StringLightCordEntity create(Level world, ICordEndpoint endpoint1, ICordEndpoint endpoint2, ItemStack item, @Nullable Float resistance) {
        if(!(item.getItem() instanceof CordItem))
            throw new IllegalArgumentException("ItemStack must be of a CordItem");
        var entity = new StringLightCordEntity(ModdedEntities.STRING_LIGHT_CORD.get(), world);
        entity.setItem((WireItem) item.getItem(), item.getCount());
        var pattern = item.get(ModdedDataComponents.LIGHT_PATTERN.get());
        if(pattern != null){
            entity.colorPattern = new int[pattern.colors().length];
            for(int i = 0; i < pattern.colors().length; ++i) {
                entity.colorPattern[i] = pattern.colors()[i].getTextureDiffuseColor();
            }
        }

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POWER, 0f);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("Filament", filamentTemperature);
        if(colorPattern != null)
            nbt.putIntArray("Pattern", colorPattern);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        filamentTemperature = nbt.getFloat("Filament");
        if(nbt.contains("Pattern")) {
            colorPattern = nbt.getIntArray("Pattern");
            if (colorPattern.length == 0) {
                colorPattern = null;
            }
        }
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
    protected boolean testForOverheat(float temperature, float energy) {
        return super.testForOverheat(temperature, energy) || filamentTemperature > 1800;
    }

    @Override
    protected boolean testForCooling(float energy) {
        return super.testForCooling(energy) && filamentPower <= 0;
    }

    @Override
    public void tick() {
        if(!level().isClientSide || level() instanceof PonderLevel) {
            if(pWire1 != null && pWire2 != null) {
                filamentPower = (float) ((pWire1.power() + pWire2.power()) / Math.max(itemCount, 1));
                var power = filamentPower - DISSIPATION_FACTOR * filamentTemperature;
                filamentTemperature += power * 0.05f / THERMAL_MASS;

                var x = Mth.clamp((filamentTemperature - 600f) / (1400f - 600f), 0, 1);
                entityData.set(POWER, x * x);
                if(level() instanceof PonderLevel) {
                    prevPower = this.power;
                    this.power = x * x;
                }
            }
        } else {
            prevPower = power;
            power = entityData.get(POWER);
        }
        super.tick();

        if(lazyTick++ >= 5) {
            lazyTick = 0;
            lazyTick();
        }
    }

    public void setLightBlocks(BlockState state) {
        var pos1 = endpoint1.getExactPosition(level());
        var pos2 = endpoint2.getExactPosition(level());
        var delta = pos2.subtract(pos1);
        var length = delta.length();
        int count = (int) (length / 0.9f) + 1;
        for(int i = 0; i < count; ++i) {
            var lightPos = BlockPos.containing(pos1.add(delta.scale((float) (i + 1) / count)));
            var current = level().getBlockState(lightPos);
            if(current.is(Blocks.AIR) || current.is(ModdedBlocks.STRING_LIGHT_BLOCK.get()))
                level().setBlock(lightPos, state, Block.UPDATE_ALL);
        }
    }

    public void lazyTick() {
        if(!level().isClientSide) {
            var x = Mth.clamp((filamentTemperature - 600f) / (1400f - 600f), 0, 1);
            var power = x * x;
            if (power > 0.75f) {
                var state = ModdedBlocks.STRING_LIGHT_BLOCK.getDefaultState();
                this.state = true;
                setLightBlocks(state);
            } else if(this.state) {
                var state = Blocks.AIR.defaultBlockState();
                this.state = false;
                setLightBlocks(state);
            }
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if(reason.shouldDestroy()) {
            setLightBlocks(Blocks.AIR.defaultBlockState());
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
        if(colorPattern == null)
            return 0xD0B070;
        var color = colorPattern[renderIndex++];
        renderIndex %= colorPattern.length;
        return color;
    }
}
