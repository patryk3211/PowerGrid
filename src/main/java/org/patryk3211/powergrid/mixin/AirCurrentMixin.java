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
package org.patryk3211.powergrid.mixin;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.Direction;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = AirCurrent.class, remap = false)
public abstract class AirCurrentMixin {
    @Shadow @Final public IAirCurrentSource source;
    @Shadow protected abstract int getLimit();
    @Shadow public Direction direction;

    @Unique
    private final List<AThermalBehaviour> powerGrid$affectedThermals = new ArrayList<>();
    @Unique
    private final List<CircuitBoardBlockEntity> powerGrid$affectedCircuits = new ArrayList<>();

    @Inject(method = "rebuild()V", at = @At("HEAD"))
    private void powerGrid$rebuildHead(CallbackInfo ci) {
        powerGrid$affectedThermals.forEach(behaviour -> behaviour.removeCoolingMultiplier((AirCurrent) (Object) this));
        powerGrid$affectedThermals.clear();
        powerGrid$affectedCircuits.forEach(behaviour -> behaviour.removeCoolingMultiplier((AirCurrent) (Object) this));
        powerGrid$affectedCircuits.clear();
    }

    @Inject(method = "rebuild()V", at = @At("TAIL"))
    private void powerGrid$rebuildTail(CallbackInfo ci) {
        var world = source.getAirCurrentWorld();
        var start = source.getAirCurrentPos();

        int limit = getLimit();
        float initialStrength = Math.abs(source.getSpeed()) * ModdedConfigs.server().kinetics.encasedFanCoolingStrength.getF();
        if(initialStrength == 0)
            return;
        for(int i = 1; i <= limit; ++i) {
            var pos = start.relative(direction, i);
            var thermal = BlockEntityBehaviour.get(world, pos, AThermalBehaviour.TYPE);
            float factor = 1.0f - (float) (i - 1) / limit;
            if(thermal != null) {
                thermal.addCoolingMultiplier((AirCurrent) (Object) this, factor * initialStrength);
                powerGrid$affectedThermals.add(thermal);
            }
            if(world.getBlockEntity(pos) instanceof CircuitBoardBlockEntity circuit) {
                circuit.addCoolingMultiplier((AirCurrent) (Object) this, factor * initialStrength);
                powerGrid$affectedCircuits.add(circuit);
            }
        }
    }
}
