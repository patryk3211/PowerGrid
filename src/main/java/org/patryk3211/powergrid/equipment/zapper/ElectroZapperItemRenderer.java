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
package org.patryk3211.powergrid.equipment.zapper;

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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.PowerGridClient;

/**
 * @see com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItemRenderer
 */
public class ElectroZapperItemRenderer extends CustomRenderedItemModelRenderer {
    protected static final PartialModel COG = PartialModel.of(PowerGrid.asResource("item/electrozapper/cog"));

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        renderer.render(model.getOriginalModel(), light);
        LocalPlayer player = mc.player;
        boolean mainHand = player.getMainHandItem() == stack;
        boolean offHand = player.getOffhandItem() == stack;
        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;

        float offset = -2.5f / 16;
        float worldTime = AnimationTickHolder.getRenderTime() / 10;
        float angle = worldTime * -25;
        float speed = PowerGridClient.ELECTRO_ZAPPER_RENDER_HANDLER.getAnimation(mainHand ^ leftHanded,
                AnimationTickHolder.getPartialTicks());

        if (mainHand || offHand)
            angle += 360 * Mth.clamp(speed * 5, 0, 1);
        angle %= 360;

        ms.pushPose();
        ms.translate(0, offset, 0);
        ms.mulPose(Axis.ZP.rotationDegrees(angle));
        ms.translate(0, -offset, 0);
        renderer.render(COG.get(), light);
        ms.popPose();
    }
}
