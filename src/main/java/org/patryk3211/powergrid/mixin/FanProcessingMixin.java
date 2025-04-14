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

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.NbtCompound;
import org.patryk3211.powergrid.electricity.heater.IProcessingTypeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FanProcessing.class)
public class FanProcessingMixin {
    @Inject(
            method = "decrementProcessingTime(Lnet/minecraft/entity/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtCompound;putInt(Ljava/lang/String;I)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private static void modifyProcessingTime(ItemEntity entity, FanProcessingType type, CallbackInfoReturnable<Integer> cir, @Local(ordinal = 2) NbtCompound processing) {
        if(type instanceof IProcessingTypeModifier modifier) {
            int time = processing.getInt("Time");
            time = modifier.modifyTime(time);
            processing.putInt("Time", time);
        }
    }
}
