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
import com.simibubi.create.compat.rei.category.animations.AnimatedKinetics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.electromagnet.ElectromagnetBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnimatedMagnet extends AnimatedKinetics {
    private static final ResourceLocation ION_POSITIVE = PowerGrid.texture("particle/positive");
    private static final ResourceLocation ION_NEGATIVE = PowerGrid.texture("particle/negative");

    private final List<Tuple<Integer, Integer>> ionPositions = new ArrayList<>();
    private Random r = new Random();

    public AnimatedMagnet() {
        for(int i = 0; i < 6; ++i) {
            ionPositions.add(new Tuple<>(
                    r.nextInt(100), r.nextInt(70)
            ));
        }
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(xOffset, yOffset, 200);

        matrixStack.pushPose();
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
        int scale = 24;//basin ? 23 : 24;

        blockElement(ModdedBlocks.ELECTROMAGNET.getDefaultState().setValue(ElectromagnetBlock.FACING, Direction.DOWN))
                .scale(scale)
                .render(graphics);
        matrixStack.popPose();

        matrixStack.translate(-5, -2, 0);
        for(int i = 0; i < ionPositions.size(); ++i) {
            matrixStack.pushPose();
            var texture = i % 2 == 0 ? ION_POSITIVE : ION_NEGATIVE;
            var pos = ionPositions.get(i);
            matrixStack.translate(pos.getA() / 4f, pos.getB() / 4f, 0);

            int x = pos.getA() + r.nextInt(-2, 3);
            int y = pos.getB() + r.nextInt(-2, 3);
            if(x < 0) x = 0;
            if(y < 0) y = 0;
            if(x > 100) x = 100;
            if(y > 70) y = 70;

            if(r.nextFloat() < 0.001f) {
                x = r.nextInt(100);
                y = r.nextInt(70);
            }
            pos.setA(x);
            pos.setB(y);

            graphics.blit(texture, 0, 0, 16, 16, 16, 16, 16, 16);
            matrixStack.popPose();
        }

        matrixStack.popPose();
    }
}
