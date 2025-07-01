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

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlock;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlockEntity;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = MechanicalMixerBlockEntity.class, remap = false)
public abstract class MechanicalMixerMixin extends BasinOperatingBlockEntity {
    @Shadow public int runningTicks;

    @Shadow public boolean running;

    @Shadow public abstract void renderParticles();

    public MechanicalMixerMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Unique
    private Optional<ChemicalVatBlockEntity> getChemicalVat() {
        var be = world.getBlockEntity(pos.down(2), ModdedBlockEntities.CHEMICAL_VAT.get());
        return be.filter(vat -> vat.getCachedState().get(ChemicalVatBlock.OPEN));
    }

    @Inject(method = "tick()V",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/mixer/MechanicalMixerBlockEntity;getSpeed()F",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void tick(CallbackInfo ci) {
        if(running && world != null) {
            var vat = getChemicalVat();
            if(vat.isEmpty())
                return;

            if(world.isClient && runningTicks == 20)
                renderParticles();

            if((!world.isClient || isVirtual()) && runningTicks == 20) {
                if(getSpeed() == 0) {
                    runningTicks = 21;
                    sendData();
                } else {
                    var turbulenceFactor = getSpeed() / 64f;
                    vat.get().applyTurbulence(Math.abs(turbulenceFactor));
                }
            }

            if(runningTicks != 20)
                runningTicks++;
            ci.cancel();
        }
    }
}
