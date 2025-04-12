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

import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.patryk3211.powergrid.electricity.heater.HeaterBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AllFanProcessingTypes.BlastingType.class)
public class BlastingTypeMixin {
    @Inject(at = @At("TAIL"), method = "isValidAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z", cancellable = true)
    private void checkHeatingCoil(World level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if(level.getBlockEntity(pos) instanceof HeaterBlockEntity heatingCoil) {
            cir.setReturnValue(heatingCoil.getState() == HeaterBlockEntity.State.BLASTING);
        }
    }
}
