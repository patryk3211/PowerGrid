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

import com.simibubi.create.foundation.blockEntity.behaviour.*;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.util.Hand;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.TransformerWindingC2SPacket;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;
import java.util.function.Supplier;

public class TransformerWindingScreen extends ValueSettingsScreen {
    public static ValueSettingsBoard makeBoard(TransformerBlock block) {
        return new ValueSettingsBoard(
                Lang.translateDirect("gui.transformer.turns"),
                block.getMaxTurns(),
                10,
                List.of(Components.literal("N")),
                new ValueSettingsFormatter(TransformerWindingScreen::formatSettings)
        );
    }

    public static MutableText formatSettings(ValueSettingsBehaviour.ValueSettings settings) {
        return Lang
                .number(Math.max(1, Math.abs(settings.value())))
                .component();
    }

    private final Hand hand;

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

    public TransformerWindingScreen(TransformerBlock block, Hand hand, int current) {
        super(null, makeBoard(block), new ValueSettingsBehaviour.ValueSettings(0, current), setting -> {});
        this.hand = hand;
    }

    protected void saveAndClose(double pMouseX, double pMouseY) {
        ValueSettingsBehaviour.ValueSettings closest = getClosestCoordinate((int) pMouseX, (int) pMouseY);
        var value = Math.max(closest.value(), 1);
        ModdedPackets.getChannel().sendToServer(new TransformerWindingC2SPacket(value, hand));
        close();
    }

    public static void clientTick() {
        if(interactionTicks == -1)
            return;

        if(++interactionTicks <= 3) {
            var mc = MinecraftClient.getInstance();
            if (!mc.options.useKey.isPressed()) {
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
