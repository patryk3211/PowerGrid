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

import com.simibubi.create.content.fluids.particle.BasinFluidParticle;
import com.simibubi.create.content.fluids.particle.FluidStackParticle;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BasinFluidParticle.class)
public abstract class BasinFluidParticleMixin extends FluidStackParticle {
    public BasinFluidParticleMixin(ClientWorld world, FluidStack fluid, double x, double y, double z, double vx, double vy, double vz) {
        super(world, fluid, x, y, z, vx, vy, vz);
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/processing/basin/BasinBlock;isBasin(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean checkVat(WorldView world, BlockPos pos) {
        return BasinBlock.isBasin(world, pos) || world.getBlockEntity(pos) instanceof ChemicalVatBlockEntity;
    }

    @Accessor(value = "yOffset", remap = false)
    public abstract float getYOffset();

    @Accessor(value = "basinPos", remap = false)
    public abstract BlockPos getBasinPos();

    @Inject(method = "tick()V", at = @At(value = "TAIL"))
    private void tickInject(CallbackInfo ci) {
        if(age % 2 == 0) {
            var blockEntity = world.getBlockEntity(getBasinPos());
            if(blockEntity instanceof ChemicalVatBlockEntity vat) {
                float level = vat.getFluidLevel();
                if(level <= 0)
                    return;
                y = 2 / 16f + getBasinPos().getY() + 13 / 16f * level + getYOffset();
            }
        }
    }
}
