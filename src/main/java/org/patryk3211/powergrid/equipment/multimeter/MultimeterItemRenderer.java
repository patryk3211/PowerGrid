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
package org.patryk3211.powergrid.equipment.multimeter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

@Environment(EnvType.CLIENT)
public class MultimeterItemRenderer extends CustomRenderedItemModelRenderer {
    private static float mainPrevDial;
    private static float mainDial;
    private static float offPrevDial;
    private static float offDial;

    private static float getDialState(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        var pt = AnimationTickHolder.getPartialTicks();
        if(player.getMainHandItem() == stack) {
            return Mth.lerp(pt, mainPrevDial, mainDial);
        } else if(player.getOffhandItem() == stack) {
            return Mth.lerp(pt, offPrevDial, offDial);
        } else {
            return 0;
        }
    }

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();
        renderer.render(model.getOriginalModel(), light);

        if(transformType.firstPerson()) {
            var angle = -Math.PI / 4 + Math.PI / 2 * getDialState(stack);
            ms.rotateAround(new Quaternionf().rotateZ((float) angle), 0, (5.75f - 8) / 16, 0);
        }
        renderer.render(ModdedPartialModels.MULTIMETER_NEEDLE.get(), light);
        ms.popPose();
    }

    public static void clientTick(Level level, Player player) {
        var stack1 = player.getMainHandItem();
        if(stack1.getItem() instanceof MultimeterItem multimeter) {
            mainPrevDial = mainDial;
            mainDial = multimeter.getDial(level, stack1);
        } else {
            mainDial = 0;
            mainPrevDial = 0;
        }
        var stack2 = player.getOffhandItem();
        if(stack2.getItem() instanceof MultimeterItem multimeter) {
            offPrevDial = offDial;
            offDial = multimeter.getDial(level, stack2);
        } else {
            offDial = 0;
            offPrevDial = 0;
        }
    }
}
