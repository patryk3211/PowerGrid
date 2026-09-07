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
package org.patryk3211.powergrid.electricity.base;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;

public abstract class AThermalBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<AThermalBehaviour> TYPE = new BehaviourType<>("thermal");
    public static final float STANDARD_TEMPERATURE = 22.0f;
    public static final float ABSOLUTE_ZERO = -273.15f;

    protected AThermalBehaviour(SmartBlockEntity be) {
        super(be);
    }

    public static float getAmbientTemperature(Level level, BlockPos pos) {
        return 13.65f * level.getBiome(pos).value().getBaseTemperature() + 7.1f;
    }
    public static boolean shouldExplode() {
        return ModdedConfigs.server().electricity.explosiveDeconstruction.get();
    }
    public static boolean shouldOverheat() {
        return ModdedConfigs.server().electricity.overheating.get();
    }

    abstract public void resetTemperature();
    abstract public float getTemperature();
    abstract public void setTemperature(float temperature);
    abstract public void addCoolingMultiplier(AirCurrent current, float value);
    abstract public void removeCoolingMultiplier(AirCurrent current);
    abstract public boolean isOverheated();
    abstract public float maxPower();

    abstract public void applyTickPower(double power);
    public void applyWirePower(@Nullable AbstractElectricWire wire) {
        if(wire == null)
            return;
        if(wire.isConverged()) {
            var network = wire.getNetwork();
            if(network != null) {
                if(wire.getNode1() != null && network.isLeaf(wire.getNode1()))
                    return;
                if(wire.getNode2() != null && network.isLeaf(wire.getNode2()))
                    return;
            }
            applyTickPower(wire.power());
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
