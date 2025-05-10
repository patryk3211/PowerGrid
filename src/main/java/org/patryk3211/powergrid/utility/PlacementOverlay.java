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

import com.simibubi.create.foundation.mixin.fabric.GuiAccessor;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.transformer.TransformerBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerSmallBlockEntity;
import org.patryk3211.powergrid.electricity.wire.WirePreview;
import org.patryk3211.powergrid.mixin.client.BlueprintOverlayRendererAccessor;

import java.util.ArrayList;
import java.util.List;

public class PlacementOverlay {
    private static final List<IOverlayTextProvider> overlayProviders = new ArrayList<>();

    public static void init() {
        overlayProviders.add(PlacementOverlay::getTransformerText);
    }

    public static void setItemRequirement(Item item, int count, boolean hasItems) {
        if(!BlueprintOverlayRendererAccessor.getActive()) {
            BlueprintOverlayRendererAccessor.setActive(true);
            BlueprintOverlayRendererAccessor.setEmpty(false);
            BlueprintOverlayRendererAccessor.setNoOutput(true);

            var ingredients = BlueprintOverlayRendererAccessor.getIngredients();
            ingredients.clear();

            for(int wires = count; wires > 0; wires -= 64) {
                ingredients.add(Pair.of(new ItemStack(item, Math.min(64, wires)), hasItems));
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static void renderOverlay(InGameHud gui, DrawContext graphics) {
        var mc = MinecraftClient.getInstance();
        if(!mc.options.hudHidden && mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
//            if(((GuiAccessor) gui).getToolHighlightTimer() <= 0) {
                MutableText text = null;

                var player = mc.player;
                for(var provider : overlayProviders) {
                    text = provider.get(player);
                    if(text != null)
                        break;
                }

                if(text != null) {
                    var window = mc.getWindow();
                    int x = (window.getScaledWidth() - gui.getTextRenderer().getWidth(text)) / 2;
                    int y = window.getScaledHeight() - 61;
                    var color = new Color(4905802);
                    graphics.drawText(gui.getTextRenderer(), text, x, y, color.getRGB(), false);
                }
//            }
        }
    }

    public static MutableText getTransformerText(PlayerEntity player) {
        var wireStack = WirePreview.getUsedWireStack(player);
        if(wireStack == null)
            return null;

        var tag = wireStack.getNbt();
        assert tag != null;
        if(!tag.contains("Turns") || !tag.contains("Initiator"))
            return null;

        var world = player.getWorld();
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
            return Lang.translateDirect("message.coil_winding_primary", Lang.number(turns).style(Formatting.WHITE).component());
        } else {
            var primaryTurns = transformer.getPrimary().getTurns();
            int largestCommonDenominator = 1;
            for(int i = 2; i <= Math.max(primaryTurns, turns); ++i) {
                if(turns % i == 0 && primaryTurns % i == 0)
                    largestCommonDenominator = i;
            }
            var n1 = Lang.number(primaryTurns / largestCommonDenominator);
            var n2 = Lang.number(turns / largestCommonDenominator);
            var ratio = n1.add(Text.of(":")).add(n2);
            return Lang.translateDirect("message.coil_winding_secondary", Lang.number(turns).style(Formatting.WHITE).component(), ratio.style(Formatting.WHITE).component());
        }
    }

    public interface IOverlayTextProvider {
        @Nullable
        MutableText get(PlayerEntity player);
    }
}
