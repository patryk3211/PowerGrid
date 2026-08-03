package org.patryk3211.powergrid.equipment.saw;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SawItemRenderer extends CustomRenderedItemModelRenderer {
    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        renderer.render(model.getOriginalModel(), light);
    }

    public static boolean renderPlayerHand(ItemStack heldItem, InteractionHand hand, PoseStack ms, MultiBufferSource buffer, int light, float pt, float swing, float equip) {
        if(heldItem.getItem() instanceof SawItem) {
            Minecraft mc = Minecraft.getInstance();
            ItemInHandRenderer firstPersonRenderer = mc.getEntityRenderDispatcher().getItemInHandRenderer();
            boolean rightHand = hand == InteractionHand.MAIN_HAND ^ mc.player.getMainArm() == HumanoidArm.LEFT;

            float flip = rightHand ? 1.0F : -1.0F;
            ms.pushPose();
            ms.translate(flip * (0.64f - 0.1f), -0.6f + equip * -0.6f, -0.72f - 0.1f);

            var rand = mc.level.random;
            ms.mulPose(Axis.YP.rotationDegrees(flip * (rand.nextFloat() * 2 - 1) * 5.0f * swing));
            ms.mulPose(Axis.ZP.rotationDegrees(flip * (rand.nextFloat() * 2 - 1) * -2.0f * swing));
            firstPersonRenderer.renderItem(mc.player, heldItem, rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !rightHand, ms, buffer, light);
            ms.popPose();
            return true;
        }
        return false;
    }
}
