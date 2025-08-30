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
package org.patryk3211.powergrid.electricity.resistor;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.NumberFormats;

import java.util.function.Consumer;

public class ResistorValueBehaviour extends ScrollValueBehaviour {
    private final int minOffset;

    public ResistorValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot, int minOffset, int max) {
        super(label, be, slot);
        this.minOffset = minOffset;
        between(0, max);
        withFormatter(i -> NumberFormats.formatPrecise(exponentialValue(minOffset, i)));
    }

    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> rows = ImmutableList.of(
                Lang.translateDirect("generic.unit.ohm")
        );
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
        return new ValueSettingsBoard(this.label, max, 9, rows, formatter);
    }

    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
        int value = Math.max(0, valueSetting.value());
        if (!valueSetting.equals(this.getValueSettings())) {
            this.playFeedbackSound(this);
        }

        this.setValue(value);
    }

    public ScrollValueBehaviour withResistanceCallback(Consumer<Float> resistanceCallback) {
        return super.withCallback(i -> resistanceCallback.accept(exponentialValue(minOffset, i)));
    }

    public static float exponentialValue(int min, int i) {
        var number = i % 9 + 1;
        var mult = Math.pow(10, i / 9 - min);
        return (float) (number * mult);
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return Lang
                .text(NumberFormats.formatPrecise(exponentialValue(minOffset, settings.value())))
                .component();
    }

    public float getResistance() {
        return exponentialValue(minOffset, value);
    }
}
