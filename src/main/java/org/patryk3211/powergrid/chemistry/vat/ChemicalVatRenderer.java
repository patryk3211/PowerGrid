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
package org.patryk3211.powergrid.chemistry.vat;

import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class ChemicalVatRenderer extends SafeBlockEntityRenderer<ChemicalVatBlockEntity> {
    public ChemicalVatRenderer(BlockEntityRendererFactory.Context context) {

    }

    @Override
    protected void renderSafe(ChemicalVatBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, int light, int overlay) {
        float fluidLevel = (float) be.getFluidAmount() / be.getFluidCapacity();
        if(fluidLevel > 0) {
            var fluids = be.getFluidStorage(null);
            float xMin = 2 / 16f, xMax = 14 / 16f,
                    yMin = 2 / 16f, yMax = yMin + 13 / 16f * fluidLevel,
                    zMin = 2 / 16f, zMax = 14 / 16f;

            long amount = 0;
            StorageView<FluidVariant> maxFluid = null;
            for(var fluid : fluids) {
                if(fluid.isResourceBlank())
                    continue;
                if(fluid.getAmount() <= 0)
                    continue;
                if(fluid.getAmount() > amount) {
                    amount = fluid.getAmount();
                    maxFluid = fluid;
                }
            }
            if(maxFluid != null) {
                FluidRenderer.renderFluidBox(new FluidStack(maxFluid), xMin, yMin, zMin, xMax, yMax, zMax, bufferSource, ms, light, false);
            }
        }
    }
}
