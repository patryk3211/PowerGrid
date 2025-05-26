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
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.utility.Directions;

import static org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlock.checkState;

public class ChemicalVatRenderer extends SafeBlockEntityRenderer<ChemicalVatBlockEntity> {
    public ChemicalVatRenderer(BlockEntityRendererFactory.Context context) {

    }

    @Override
    protected void renderSafe(ChemicalVatBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, int light, int overlay) {
        float fluidLevel = (float) be.getFluidAmount() / be.getFluidCapacity();
        if(fluidLevel > 0) {
            float xMin = 2 / 16f, xMax = 14 / 16f,
                    yMin = 2 / 16f, yMax = yMin + 13 / 16f * fluidLevel,
                    zMin = 2 / 16f, zMax = 14 / 16f;

            var pos = be.getPos();
            var world = be.getWorld();
            var block = be.getCachedState().getBlock();
            if(!checkState(block, world.getBlockState(pos.west())))
                xMin = 0.0f;
            if(!checkState(block, world.getBlockState(pos.east())))
                xMax = 1.0f;
            if(!checkState(block, world.getBlockState(pos.north())))
                zMin = 0.0f;
            if(!checkState(block, world.getBlockState(pos.south())))
                zMax = 1.0f;

            var rendered = be.getRenderedFluid();
            if(rendered != null) {
                var variant = rendered.getResource();
                var sprites = FluidVariantRendering.getSprites(variant);
                var fluidTexture = sprites != null ? sprites[0] : null;
                if (fluidTexture == null)
                    return;

                int color = FluidVariantRendering.getColor(variant);
                var buffer = FluidRenderer.getFluidBuilder(bufferSource);
                FluidRenderer.renderStillTiledFace(Direction.UP, xMin, zMin, xMax, zMax, yMax, buffer, ms, light, color, fluidTexture);

                for(var dir : Directions.HORIZONTAL) {
                    var nBE = world.getBlockEntity(pos.offset(dir));
                    if(!(nBE instanceof ChemicalVatBlockEntity vat))
                        continue;
                    float neighborLevel = (float) vat.getFluidAmount() / vat.getFluidCapacity();
                    float levelDiff = (fluidLevel - neighborLevel) * 13 / 16f;
                    if(levelDiff < 0)
                        continue;
                    float depth = 0.0f;
                    if(dir.getDirection() == Direction.AxisDirection.POSITIVE)
                        depth = 1.0f;
                    FluidRenderer.renderStillTiledFace(dir, 0.0f, yMax - levelDiff, 1.0f, yMax, depth, buffer, ms, light, color, fluidTexture);
                }
            }
        }
    }
}
