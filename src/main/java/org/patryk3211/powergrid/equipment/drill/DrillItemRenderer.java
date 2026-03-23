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
package org.patryk3211.powergrid.equipment.drill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.PowerGrid;

public class DrillItemRenderer extends CustomRenderedItemModelRenderer {
    protected static final PartialModel HEAD = PartialModel.of(PowerGrid.asResource("item/drill/head"));

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        renderer.render(model.getOriginalModel(), light);
        LocalPlayer player = mc.player;
        boolean mainHand = player.getMainHandItem() == stack;

        float offset = -2.5f / 16;
        float worldTime = AnimationTickHolder.getRenderTime() / 10;
        float angle = 0; //worldTime * -25;
        float speed = 0.0f;
        if(player instanceof PlayerDrillExtensions ext) {
            angle = -ext.powerGrid$animation(AnimationTickHolder.getPartialTicks());
//            speed += ext.powerGrid$drillSpeedMultiplier() * 0.5f;
        }

//        if (mainHand)
//            angle += 360 * speed * 5; //Mth.clamp(speed * 5, 0, 1);
//        angle %= 360;

        ms.pushPose();
        ms.translate(0, offset, 0);
        ms.mulPose(Axis.ZP.rotationDegrees(angle));
        ms.translate(0, -offset, 0);
        renderer.render(HEAD.get(), light);
        ms.popPose();
    }
}
