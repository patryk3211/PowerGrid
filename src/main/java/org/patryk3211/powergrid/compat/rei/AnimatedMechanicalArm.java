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
package org.patryk3211.powergrid.compat.rei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.rei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class AnimatedMechanicalArm extends AnimatedKinetics {
    public Supplier<ItemStack> heldItemSupplier;
    private ItemStack heldItem = ItemStack.EMPTY;
    private boolean grabbed = false;

    private static float animationPart(float tick, float start, float duration) {
        return Mth.clamp((tick - start) / duration, 0, 1);
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(xOffset, yOffset, 200);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
        int scale = 23;
        matrixStack.scale(scale, scale, scale);

        blockElement(AllBlocks.MECHANICAL_ARM.getDefaultState())
                .atLocal(0, 1.5f, 0)
                .render(graphics);

        blockElement(AllPartialModels.ARM_COG)
                .rotateBlock(0, getCurrentAngle() * 2, 0)
                .atLocal(0, 1.5f, 0)
                .render(graphics);

        var trStack = TransformStack.of(matrixStack);
        trStack.center();

        // The whole animation takes 65 ticks (about 3 seconds)
        float animationTick = AnimationTickHolder.getRenderTime() % 65;
        float lowerArmAnimation =
                  animationPart(animationTick, 5, 10)
                - animationPart(animationTick, 15, 10)
                + animationPart(animationTick, 35, 10)
                - animationPart(animationTick, 45, 10);
        float baseAnimation =
                  animationPart(animationTick, 25, 10)
                - animationPart(animationTick, 55, 10);

        if(!grabbed && animationTick >= 15 && animationTick < 30) {
            grabbed = true;
            if(heldItemSupplier != null)
                heldItem = heldItemSupplier.get();
        } else if(grabbed && animationTick >= 45) {
            grabbed = false;
            heldItem = ItemStack.EMPTY;
        }

        boolean hasItem = !heldItem.isEmpty();
        var itemRenderer = Minecraft.getInstance()
                .getItemRenderer();

        var bakedModel = itemRenderer.getModel(heldItem, null, null, 0);
        var isBlockItem = hasItem && (heldItem.getItem() instanceof BlockItem) && bakedModel.isGui3d();

        ArmRenderer.transformBase(trStack, -90 + baseAnimation * 180);
        blockElement(AllPartialModels.ARM_BASE)
                .render(graphics);

        trStack.translate(0, -0.25f, 0);
        ArmRenderer.transformLowerArm(trStack, 180 - lowerArmAnimation * 90f);
        blockElement(AllPartialModels.ARM_LOWER_BODY)
                .atLocal(0, 0, 0)
                .render(graphics);

        ArmRenderer.transformUpperArm(trStack, -45 + lowerArmAnimation * 75f);
        blockElement(AllPartialModels.ARM_UPPER_BODY)
                .render(graphics);

        ArmRenderer.transformHead(trStack, 0);
        blockElement(AllPartialModels.ARM_CLAW_BASE)
                .render(graphics);

        trStack.pushPose();
        ArmRenderer.transformClawHalf(trStack, hasItem, isBlockItem, -1);
        blockElement(AllPartialModels.ARM_CLAW_GRIP_LOWER)
                .render(graphics);

        trStack.popPose();
        trStack.pushPose();

        ArmRenderer.transformClawHalf(trStack, hasItem, isBlockItem, 1);
        blockElement(AllPartialModels.ARM_CLAW_GRIP_UPPER)
                .render(graphics);

        trStack.popPose();

        if(hasItem) {
            trStack.pushPose();
            float itemScale = isBlockItem ? .5f : .625f;
            trStack.rotateXDegrees(90);
            trStack.translate(0, isBlockItem ? -9 / 16f : -10 / 16f, 0);
            trStack.scale(itemScale, itemScale, itemScale);

            itemRenderer.render(heldItem, ItemDisplayContext.FIXED, false, matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, bakedModel);
            trStack.popPose();
        }

        matrixStack.popPose();
    }
}
