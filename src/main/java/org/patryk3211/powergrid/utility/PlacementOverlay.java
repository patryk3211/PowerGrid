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
package org.patryk3211.powergrid.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.theme.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.electricswitch.SwitchBlock;
import org.patryk3211.powergrid.electricity.info.TerminalHandler;
import org.patryk3211.powergrid.electricity.modulardisplay.ModularDisplayBlockEntity;
import org.patryk3211.powergrid.electricity.transformer.TransformerBlock;
import org.patryk3211.powergrid.electricity.wire.WirePreview;
import org.patryk3211.powergrid.equipment.multimeter.MultimeterItemRenderer;
import org.patryk3211.powergrid.equipment.thermometer.ThermometerItemRenderer;
import org.patryk3211.powergrid.mixin.client.BlueprintOverlayRendererAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

;

@Environment(EnvType.CLIENT)
public class PlacementOverlay {
    private static final List<IOverlayTextProvider> overlayProviders = new ArrayList<>();
    private static final List<Component> lines = new ArrayList<>();
    private static int overlayTicks = -1;
    public static boolean thisActivation = false;

    public static void init() {
        overlayProviders.add(PlacementOverlay::getTransformerText);
        overlayProviders.add(MultimeterItemRenderer::multimeterOverlayText);
        overlayProviders.add(WirePreview::distanceOverlay);
        overlayProviders.add(TerminalHandler::overlayText);
        overlayProviders.add(ThermometerItemRenderer::overlayText);
        overlayProviders.add(SwitchBlock::overlayText);
        overlayProviders.add(ModularDisplayBlockEntity::overlayText);
    }

    public static void setItemRequirement(Item item, int count, boolean hasItems) {
        if(!BlueprintOverlayRendererAccessor.getActive() || !thisActivation) {
            BlueprintOverlayRendererAccessor.setActive(true);
            BlueprintOverlayRendererAccessor.setEmpty(false);
            BlueprintOverlayRendererAccessor.setNoOutput(true);
            thisActivation = true;

            var ingredients = BlueprintOverlayRendererAccessor.getIngredients();
            ingredients.clear();

            for(int wires = count; wires > 0; wires -= 64) {
                ingredients.add(Pair.of(new ItemStack(item, Math.min(64, wires)), hasItems));
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static void renderOverlay(Gui gui, GuiGraphics graphics) {
        var mc = Minecraft.getInstance();
        if(!mc.options.hideGui && mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            var player = mc.player;
            boolean added = false;
            for(var provider : overlayProviders) {
                var text = provider.get(player);
                if(text == null)
                    continue;
                if(!added) {
                    lines.clear();
                    added = true;
                }
                lines.add(text);
            }

            if(added) {
                if(overlayTicks < 10) {
                    ++overlayTicks;
                }
            } else if(!lines.isEmpty()) {
                if(--overlayTicks <= 0) {
                    overlayTicks = 0;
                }
            }

            if(!lines.isEmpty()) {
                var window = mc.getWindow();
                int y = window.getGuiScaledHeight() - 61;
                var color = new Color(0x4adb4a);
                float alpha = Mth.clamp(overlayTicks, 0, 10) / 10.0f;
                var state = Arrays.copyOf(RenderSystem.getShaderColor(), 4);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                int i = 0;
                for(var text : lines) {
                    int x = (window.getGuiScaledWidth() - gui.getFont().width(text)) / 2;
                    graphics.drawString(gui.getFont(), text, x, y, color.getRGB(), true);
                    y += 11;
                    if(++i == 2)
                        break;
                }
                RenderSystem.setShaderColor(state[0], state[1], state[2], state[3]);
            }
        }
    }

    public static Component getTransformerText(Player player) {
        var wireStack = WirePreview.getUsedWireStack(player);
        if(wireStack == null)
            return null;

        var tag = wireStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if(!tag.contains("Turns") || !tag.contains("Initiator"))
            return null;

        var world = player.level();
        var posArray = tag.getIntArray("Initiator");
        var initiatorPos = new BlockPos(posArray[0], posArray[1], posArray[2]);

        var state = world.getBlockState(initiatorPos);
        if(!(state.getBlock() instanceof TransformerBlock transformerBlock))
            return null;

        var transformerOpt = transformerBlock.getBlockEntity(world, initiatorPos, state);
        if(transformerOpt.isEmpty())
            return null;
        var transformer = transformerOpt.get();

        var turns = tag.getInt("Turns");
        if(!transformer.hasPrimary()) {
            return Lang.translateDirect("message.coil_winding_primary", Lang.number(turns).style(ChatFormatting.WHITE).component());
        } else {
            var primaryTurns = transformer.getPrimary().getTurns();
            int largestCommonDenominator = 1;
            for(int i = 2; i <= Math.max(primaryTurns, turns); ++i) {
                if(turns % i == 0 && primaryTurns % i == 0)
                    largestCommonDenominator = i;
            }
            var n1 = Lang.number(primaryTurns / largestCommonDenominator);
            var n2 = Lang.number(turns / largestCommonDenominator);
            var ratio = n1.add(Component.nullToEmpty(":")).add(n2);
            return Lang.translateDirect("message.coil_winding_secondary", Lang.number(turns).style(ChatFormatting.WHITE).component(), ratio.style(ChatFormatting.WHITE).component());
        }
    }

    public interface IOverlayTextProvider {
        @Nullable
        Component get(Player player);
    }
}
