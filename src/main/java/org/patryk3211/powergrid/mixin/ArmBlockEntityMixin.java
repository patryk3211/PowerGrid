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

import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ArmBlockEntity.class)
public abstract class ArmBlockEntityMixin {
    @Shadow(remap = false) float chasedPointProgress;
    @Shadow(remap = false) ArmBlockEntity.Phase phase;
    @Shadow(remap = false) @Nullable protected abstract ArmInteractionPoint getTargetedInteractionPoint();

    @Shadow(remap = false) List<ArmInteractionPoint> outputs;

    @Inject(method = "tick()V", at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmBlockEntity;tickMovementProgress()Z"))
    private void tickLockCircuitOnBelt(CallbackInfo ci) {
        for(var point : outputs) {
            var beltBE = BeltHelper.getSegmentBE(point.getLevel(), point.getPos());
            if (beltBE == null)
                return;
            var transport = beltBE.getBehaviour(TransportedItemStackHandlerBehaviour.TYPE);
            if (transport == null)
                return;
            var found = new MutableBoolean(false);
            transport.handleCenteredProcessingOnAllItems(0.05f, tis -> {
                if (found.isTrue())
                    return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
                if (ModdedItems.INCOMPLETE_CIRCUIT.isIn(tis.stack)) {
                    tis.lockedExternally = true;
                    found.setTrue();
                }
                return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
            });
        }
    }
}
