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

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import com.simibubi.create.foundation.gui.ScreenOpener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CustomValueSettingsScreen extends ValueSettingsScreen {
    public static ValueSettingsBoard makeBoard(Text title, int maxValue, int milestoneInterval, List<Text> rows) {
        return new ValueSettingsBoard(title, maxValue, milestoneInterval, rows, new ValueSettingsFormatter(CustomValueSettingsScreen::simpleFormat));
    }

    public static MutableText simpleFormat(ValueSettingsBehaviour.ValueSettings settings) {
        return Lang.number(settings.value()).component();
    }

    private static int interactionTicks = -1;
    private static CustomValueSettingsScreen screen = null;

    public static boolean beginInteraction(Supplier<CustomValueSettingsScreen> screenConstructor) {
        if(interactionTicks == -1) {
            interactionTicks = 0;
            screen = screenConstructor.get();
            return true;
        }
        return false;
    }

    // This is always called on server side
    private final Consumer<ValueSettingsBehaviour.ValueSettings> saveCallback;

    public CustomValueSettingsScreen(BlockPos pos, ValueSettingsBoard board, ValueSettingsBehaviour.ValueSettings valueSettings, Consumer<ValueSettingsBehaviour.ValueSettings> saveCallback) {
        super(pos, board, valueSettings, $ -> {});
        this.saveCallback = saveCallback;
    }

    @Override
    protected void saveAndClose(double pMouseX, double pMouseY) {
        ValueSettingsBehaviour.ValueSettings closest = getClosestCoordinate((int) pMouseX, (int) pMouseY);
        saveCallback.accept(closest);
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
