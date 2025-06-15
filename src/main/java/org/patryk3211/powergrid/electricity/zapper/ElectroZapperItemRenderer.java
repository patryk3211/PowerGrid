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
package org.patryk3211.powergrid.electricity.zapper;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.PowerGridClient;

/**
 * @see com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItemRenderer
 */
public class ElectroZapperItemRenderer extends CustomRenderedItemModelRenderer {
    protected static final PartialModel COG = new PartialModel(PowerGrid.asResource("item/electrozapper/cog"));

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ModelTransformationMode transformType, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
        MinecraftClient mc = MinecraftClient.getInstance();
        renderer.render(model.getOriginalModel(), light);
        ClientPlayerEntity player = mc.player;
        boolean mainHand = player.getMainHandStack() == stack;
        boolean offHand = player.getOffHandStack() == stack;
        boolean leftHanded = player.getMainArm() == Arm.LEFT;

        float offset = -2.5f / 16;
        float worldTime = AnimationTickHolder.getRenderTime() / 10;
        float angle = worldTime * -25;
        float speed = PowerGridClient.ELECTRO_ZAPPER_RENDER_HANDLER.getAnimation(mainHand ^ leftHanded,
                AnimationTickHolder.getPartialTicks());

        if (mainHand || offHand)
            angle += 360 * MathHelper.clamp(speed * 5, 0, 1);
        angle %= 360;

        ms.push();
        ms.translate(0, offset, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        ms.translate(0, -offset, 0);
        renderer.render(COG.get(), light);
        ms.pop();
    }
}
