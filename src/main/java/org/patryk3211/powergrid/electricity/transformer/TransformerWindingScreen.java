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
package org.patryk3211.powergrid.electricity.transformer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.TransformerWindingC2SPacket;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;
import java.util.function.Supplier;

public class TransformerWindingScreen extends ValueSettingsScreen {
    public static final ResourceLocation CAP_TEXTURE = PowerGrid.texture("gui/brass_cover");

    public static ValueSettingsBoard makeBoard(TransformerBlock block) {
        return new ValueSettingsBoard(
                Lang.translateDirect("gui.transformer.turns"),
                block.getMaxTurns(),
                10,
                List.of(Component.literal("N")),
                new ValueSettingsFormatter(TransformerWindingScreen::formatSettings)
        );
    }

    public static MutableComponent formatSettings(ValueSettingsBehaviour.ValueSettings settings) {
        return Lang
                .number(Math.max(1, Math.abs(settings.value())))
                .component();
    }

    private final InteractionHand hand;
    private int cap;

    private static int interactionTicks = -1;
    private static TransformerWindingScreen screen = null;

    public static boolean beginInteraction(Supplier<TransformerWindingScreen> potentialScreen) {
        if(interactionTicks == -1) {
            interactionTicks = 0;
            screen = potentialScreen.get();
            return true;
        }
        return false;
    }

    public TransformerWindingScreen(TransformerBlock block, InteractionHand hand, int current, int primaryTurns) {
        super(null, makeBoard(block), new ValueSettingsBehaviour.ValueSettings(0, current), setting -> {}, 1000);
        this.hand = hand;
        this.cap = block.getMaxTurns() - primaryTurns;
    }

    @Override
    public ValueSettingsBehaviour.ValueSettings getClosestCoordinate(int mouseX, int mouseY) {
        var value = super.getClosestCoordinate(mouseX, mouseY);
        if(value.value() > cap)
            return new ValueSettingsBehaviour.ValueSettings(value.row(), cap);
        return value;
    }

    public static void renderCropped(GuiGraphics graphics, int x, int y, int width, int height, int u, int v) {
        var left = x;
        var top = y;
        var right = left + width;
        var bot = top + height;

        var u1 = u / 256f;
        var v1 = v / 256f;
        var u2 = u1 + width / 256f;
        var v2 = v1 + height / 256f;

        var m = graphics.pose().last().pose();
        var c = Color.WHITE;
        int z = 0;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        bufferbuilder.vertex(m, (float) left , (float) bot, (float) z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).uv(u1, v2).endVertex();
        bufferbuilder.vertex(m, (float) right, (float) bot, (float) z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).uv(u2, v2).endVertex();
        bufferbuilder.vertex(m, (float) right, (float) top, (float) z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).uv(u2, v1).endVertex();
        bufferbuilder.vertex(m, (float) left , (float) top, (float) z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).uv(u1, v1).endVertex();
        tesselator.end();
        RenderSystem.disableBlend();
    }

    public void renderBarCap(GuiGraphics graphics, int x, int y, int width, ValueSettingsBoard board) {
        int milestoneCount = board.maxValue() / board.milestoneInterval();
        int milestoneSegmentWidth = width / milestoneCount;
        int scale = board.maxValue() > 128 ? 1 : 2;

        // Last not covered milestone
        var milestone = cap / board.milestoneInterval();
        // First milestone is never covered
        int toMilestoneOffset = milestoneSegmentWidth * milestone + 8 / scale;
        var milestoneFraction = (float) (cap - milestone * board.milestoneInterval()) / board.milestoneInterval();

        int offset = (int) (toMilestoneOffset + (milestoneSegmentWidth - 7 + 1) * milestoneFraction);
        x += offset;
        x -= 1;
        width -= offset;
        width += 2;
        if(width <= 2)
            return;

        RenderSystem.setShaderTexture(0, CAP_TEXTURE);
        int sideWidth = Math.min(3, width / 2);
        int centerWidth = width - sideWidth * 2;

        // Render sides
        renderCropped(graphics, x, y, sideWidth, 10, 0, 0);
        renderCropped(graphics, x + sideWidth + centerWidth, y, Math.min(3, width - sideWidth), 10, 253, 0);

        // Render repeated center
        for (int w = 0; w < centerWidth; w += 250 - 1) {
            var segLen = Math.min(250 - 1, centerWidth - w);
            var segX = x + w + sideWidth;
            renderCropped(graphics, segX, y, segLen, 10, 3, 0);
        }
    }

    public void renderBarCapMilestone(GuiGraphics graphics, int x, int y, int milestone, ValueSettingsBoard board) {
        var milestoneValue = milestone * board.milestoneInterval();
        if(milestoneValue > cap) {
            graphics.blit(CAP_TEXTURE, x, y + 1, 0, 11, 7, 8);
        }
    }

    protected void saveAndClose(double pMouseX, double pMouseY) {
        ValueSettingsBehaviour.ValueSettings closest = getClosestCoordinate((int) pMouseX, (int) pMouseY);
        var value = Math.max(closest.value(), 1);
        ModdedPackets.getChannel().sendToServer(new TransformerWindingC2SPacket(value, hand));
        onClose();
    }

    public static void clientTick() {
        if(interactionTicks == -1)
            return;

        if(++interactionTicks <= 3) {
            var mc = Minecraft.getInstance();
            if (!mc.options.keyUse.isDown()) {
                interactionTicks = -1;
                return;
            }

            if(interactionTicks == 3){
                ScreenOpener.open(screen);
            }
        } else {
            interactionTicks = -1;
        }
    }
}
