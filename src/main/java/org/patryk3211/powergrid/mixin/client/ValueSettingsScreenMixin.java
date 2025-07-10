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
package org.patryk3211.powergrid.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import net.minecraft.client.gui.DrawContext;
import org.patryk3211.powergrid.electricity.transformer.TransformerWindingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ValueSettingsScreen.class, remap = false)
public abstract class ValueSettingsScreenMixin {
    @Shadow private int maxLabelWidth;
    @Shadow private int valueBarWidth;
    @Shadow private ValueSettingsBoard board;

    @Inject(method = "renderWindow(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I",
            ordinal = 2))
    private void renderBar(DrawContext graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci, @Local(name = "x") int x, @Local(name = "y") int y) {
        if(!((Object) this instanceof TransformerWindingScreen screen))
            return;
        int valueBarX = x + maxLabelWidth + 14 + 4;
        screen.renderBarCap(graphics, valueBarX, y, valueBarWidth, board);
    }

    @Inject(method = "renderWindow(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/gui/AllGuiTextures;render(Lnet/minecraft/client/gui/DrawContext;II)V",
            ordinal = 1,
            shift = At.Shift.AFTER))
    private void renderMilestone(DrawContext graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci, @Local(name = "milestoneX") int x, @Local(name = "y") int y, @Local(name = "milestone") int milestone) {
        if(!((Object) this instanceof TransformerWindingScreen screen))
            return;
        screen.renderBarCapMilestone(graphics, x, y, milestone, board);
    }
}
