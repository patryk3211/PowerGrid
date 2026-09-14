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
package org.patryk3211.powergrid.equipment.thermometer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

public class ThermometerItemRenderer extends CustomRenderedItemModelRenderer {
    private static final RandomSource random = RandomSource.create();
    private static float progress = 0;
    private static float prevProgress = 0;
    private static Float temperature = null;

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        var item = ModdedBlocks.THERMOMETER.asItem();
        if(player != null && (player.getMainHandItem().is(item) || player.getOffhandItem().is(item))) {
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                var hit = (BlockHitResult) mc.hitResult;
                var behaviour = BlockEntityBehaviour.get(mc.level, hit.getBlockPos(), AThermalBehaviour.TYPE);
                if(behaviour != null) {
                    var target = Mth.clamp((behaviour.getTemperature() - 22f) / (175f - 22f), 0, 1.125f);
                    goTo(target);
                    temperature = behaviour.getTemperature();
                    return;
                }
            }
        }
        temperature = null;
        goTo(0);
    }

    private static void goTo(float target) {
        prevProgress = progress;
        progress += (target - progress) * .125f;
        if (progress > 1 && random.nextFloat() < 1 / 2f)
            progress -= (progress - 1) * random.nextFloat();
    }

    @Nullable
    public static Component overlayText(Player player) {
        if(temperature == null || !GogglesItem.isWearingGoggles(player))
            return null;
        var color = ChatFormatting.GREEN;
        if(temperature > 150) {
            color = ChatFormatting.RED;
        } else if(temperature > 125) {
            color = ChatFormatting.YELLOW;
        }
        var temperatureText = Lang.numberConstant(temperature);
        if(temperature > 175) {
            temperatureText = Lang.text(">175.0");
        }
        return Lang.translate("gui.thermometer.temperature").style(ChatFormatting.WHITE)
                .add(temperatureText.style(color)).add(Component.literal(" "))
                .add(Unit.TEMPERATURE.get()).component();
    }

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        renderer.render(model.getOriginalModel(), light);

        ms.pushPose();
        if(transformType.firstPerson()) {
            var player = mc.player;
            if(player != null && (player.getMainHandItem() == stack || player.getOffhandItem() == stack)) {
                if(Float.isNaN(progress)) {
                    progress = 0;
                    prevProgress = 0;
                }
                var angle = ThermometerRenderer.NEEDLE_SPAN * -Mth.lerp(AnimationTickHolder.getPartialTicks(), prevProgress, progress);
                ms.rotateAround(new Quaternionf().rotateZ(angle), 0, -2f / 16f, 0);
            }
        }

        renderer.render(ModdedPartialModels.THERMOMETER_NEEDLE.get(), light);
        ms.popPose();
    }
}
