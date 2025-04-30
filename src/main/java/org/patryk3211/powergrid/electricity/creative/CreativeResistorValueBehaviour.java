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
package org.patryk3211.powergrid.electricity.creative;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PreciseNumberFormat;

public class CreativeResistorValueBehaviour extends ScrollValueBehaviour {
    public CreativeResistorValueBehaviour(Text label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        between(0, 72);
        withFormatter(i -> PreciseNumberFormat.format(exponentialValue(i)));
    }

    public ValueSettingsBoard createBoard(PlayerEntity player, BlockHitResult hitResult) {
        ImmutableList<Text> rows = ImmutableList.of(
                Lang.translateDirect("generic.unit.ohm")
        );
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
        return new ValueSettingsBoard(this.label, 72, 9, rows, formatter);
    }

    public void setValueSettings(PlayerEntity player, ValueSettings valueSetting, boolean ctrlHeld) {
        int value = Math.max(0, valueSetting.value());
        if (!valueSetting.equals(this.getValueSettings())) {
            this.playFeedbackSound(this);
        }

        this.setValue(value);
    }

    public ValueSettings getValueSettings() {
        return new ValueSettings(0, this.value);
    }

    public static float exponentialValue(int i) {
        var number = i % 9 + 1;
        var mult = Math.pow(10, i / 9 - 3);
        return (float) (number * mult);
    }

    public MutableText formatSettings(ValueSettings settings) {
        return Lang
                .text(PreciseNumberFormat.format(exponentialValue(settings.value())))
                .component();
    }
}
