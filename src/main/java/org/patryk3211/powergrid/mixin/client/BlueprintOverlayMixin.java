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
package org.patryk3211.powergrid.mixin.client;

import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import org.patryk3211.powergrid.utility.PlacementOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlueprintOverlayRenderer.class)
public abstract class BlueprintOverlayMixin {
    @Inject(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getInstance()Lnet/minecraft/client/Minecraft;",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private static void preventDeactivation(CallbackInfo ci) {
        if(PlacementOverlay.thisActivation) {
            PlacementOverlay.thisActivation = false;
            ci.cancel();
        }
    }
}
