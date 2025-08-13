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
import com.simibubi.create.AllSoundEvents;
import org.patryk3211.powergrid.PowerGrid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AllSoundEvents.SoundEntryBuilder.class, remap = false)
public class SoundEntryBuilderMixin {
    @Inject(method = "build",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
            cancellable = true)
    private void earlyReturn(CallbackInfoReturnable<AllSoundEvents.SoundEntry> cir, @Local AllSoundEvents.SoundEntry entry) {
        // Avoid adding outside entries into the Create's sound entry collection
        if(entry.getId().getNamespace().equals(PowerGrid.MOD_ID))
            cir.setReturnValue(entry);
    }
}
