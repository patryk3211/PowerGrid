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
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.utility.Lang;

import java.util.function.Consumer;

public class CreativeSourceValueBehaviour extends ScrollValueBehaviour {
    private final float multiplier;
    private Consumer<Float> callback;

    public CreativeSourceValueBehaviour(Component label, SmartBlockEntity be, float multiplier, ValueBoxTransform slot) {
        super(label, be, slot);
        this.multiplier = multiplier;
        between(-500, 500);
        withFormatter(i -> String.format("%.1f", Math.abs(processValue(i))));
    }

    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> rows = ImmutableList.of(
                Component.literal("× +100"),
                Component.literal("+"),
                Component.literal("-"),
                Component.literal("× -100")
        );
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
        return new ValueSettingsBoard(this.label, 250, 20, rows, formatter);
    }

    public void setValueSettings(Player player, ValueSettingsBehaviour.ValueSettings valueSetting, boolean ctrlHeld) {
        int value = Math.max(0, valueSetting.value());
        if (!valueSetting.equals(this.getValueSettings())) {
            this.playFeedbackSound(this);
        }
        if(value == 0) {
            this.setValue(0);
            return;
        }
        this.setValue(switch(valueSetting.row()) {
            case 0 -> value + 250;
            case 1 -> value;
            case 2 -> -value;
            case 3 -> -value - 250;
            default -> throw new IllegalStateException();
        });
    }

    public void withMultipliedCallback(Consumer<Float> callback) {
        this.callback = callback;
    }

    private float processValue(int i) {
        if(i > 250) {
            i = (i - 250) * 100;
        } else if(i < -250) {
            i = (i + 250) * 100;
        }
        return i * multiplier;
    }

    public float getMultipliedValue() {
        return processValue(getValue());
    }

    @Override
    public void setValue(int value) {
        super.setValue(value);
        if(callback != null)
            callback.accept(getMultipliedValue());
    }

    public ValueSettingsBehaviour.ValueSettings getValueSettings() {
        int i = 0, row = 0;
        if(value < -250) {
            row = 3;
            i = -value - 250;
        } else if(value < 0) {
            row = 2;
            i = -value;
        } else if(value <= 250) {
            row = 1;
            i = value;
        } else {
            row = 0;
            i = value - 250;
        }
        return new ValueSettingsBehaviour.ValueSettings(row, i);
    }

    public MutableComponent formatSettings(ValueSettingsBehaviour.ValueSettings settings) {
        return Lang
                .number(Math.max(0, Math.abs(settings.value() * multiplier)))
                .component();
    }
}
