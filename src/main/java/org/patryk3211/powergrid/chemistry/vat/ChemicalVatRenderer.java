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

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;
import org.patryk3211.powergrid.chemistry.vat.upgrade.ChemicalVatUpgrade;
import org.patryk3211.powergrid.utility.Directions;

import java.util.HashSet;
import java.util.Map;

import static org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlock.*;

public class ChemicalVatRenderer extends SafeBlockEntityRenderer<ChemicalVatBlockEntity> {
    public ChemicalVatRenderer(BlockEntityRendererFactory.Context context) {

    }

    @Override
    protected void renderSafe(ChemicalVatBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, int light, int overlay) {
        // Render upgrades
        for(var upgradeEntry : be.upgrades.entrySet()) {
            var upgrade = (ChemicalVatUpgrade) upgradeEntry.getValue().getItem();
            upgrade.render(be, partialTicks, ms, bufferSource, upgradeEntry.getValue(), upgradeEntry.getKey(), light, overlay);
        }

        renderItems(be, ms, bufferSource, light, overlay);
        renderFluid(be, ms, bufferSource, light);
    }

    private void renderItems(ChemicalVatBlockEntity be, MatrixStack ms, VertexConsumerProvider bufferSource, int light, int overlay) {
        var mixture = be.getReagentMixture();
        var solids = new HashSet<Reagent>();

        int itemCount = 0;
        for(var reagent : mixture.getReagents()) {
            if(mixture.getState(reagent) != ReagentState.SOLID)
                continue;
            if(reagent.asItem() == null)
                continue;
            solids.add(reagent);
            itemCount += 1;
        }

        float angle = 0;
        for(var reagent : solids) {
            var item = reagent.asItem();
            ms.push();

            var radians = angle / 180f * Math.PI;
            ms.translate(0.5f + Math.sin(radians) * 0.1f, 0.2f, 0.5f + Math.cos(radians) * 0.1f);
            TransformStack.cast(ms)
                    .rotateY(angle)
                    .rotateX(65);

            var count = (float) mixture.getAmount(reagent) / reagent.getItemAmount();
            if(count < 1) {
                ms.scale(count, count, count);
            }

            renderItem(ms, bufferSource, light, overlay, new ItemStack(item, (int) Math.ceil(count)));

            angle += 360f / itemCount;
            ms.pop();
        }
    }

    private void renderItem(MatrixStack ms, VertexConsumerProvider bufferSource, int light, int overlay, ItemStack stack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND, light, overlay, ms, bufferSource, mc.world, 0);
    }

    private void renderFluid(ChemicalVatBlockEntity be, MatrixStack ms, VertexConsumerProvider bufferSource, int light) {
        float fluidLevel = Math.min(be.getFluidLevel(), 1);
        if(fluidLevel > 0) {
            float xMin = CORNER, xMax = CORNER + SIDE,
                    yMin = CORNER, yMax = yMin + FLUID_SPAN * fluidLevel,
                    zMin = CORNER, zMax = CORNER + SIDE;

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
                    float neighborLevel = Math.min(vat.getFluidLevel(), 1);
                    float levelDiff = (fluidLevel - neighborLevel) * FLUID_SPAN;
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
