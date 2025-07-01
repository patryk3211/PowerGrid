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

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.recipe.Recipe;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlockEntity;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BasinOperatingBlockEntity.class)
public abstract class BasinOperatingMixin extends KineticBlockEntity {
    @Shadow protected Recipe<?> currentRecipe;

    public BasinOperatingMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Inject(method = "updateBasin()Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/processing/basin/BasinOperatingBlockEntity;getBasin()Ljava/util/Optional;"),
            cancellable = true)
    private void updateBasinCheckForVat(CallbackInfoReturnable<Boolean> cir) {
        if(!(((Object) this) instanceof MechanicalMixerBlockEntity))
            return;
        var vat = getChemicalVat();
        if(vat.isPresent()) {
            startProcessingVat();
            sendData();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void startProcessingVat() {
        var mixer = (MechanicalMixerBlockEntity) (Object) this;
        if(mixer.running && mixer.runningTicks <= 20)
            return;
        currentRecipe = null;
        mixer.running = true;
        mixer.runningTicks = 0;
    }

    @Unique
    private Optional<ChemicalVatBlockEntity> getChemicalVat() {
        return world.getBlockEntity(pos.down(2), ModdedBlockEntities.CHEMICAL_VAT.get());
    }
}
