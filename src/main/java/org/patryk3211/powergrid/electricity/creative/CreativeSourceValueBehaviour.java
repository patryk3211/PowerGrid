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

public class CreativeSourceValueBehaviour extends ScrollValueBehaviour {
    private final float multiplier;

    public CreativeSourceValueBehaviour(Component label, SmartBlockEntity be, float multiplier, ValueBoxTransform slot) {
        super(label, be, slot);
        this.multiplier = multiplier;
        between(-1000, 1002);
        withFormatter(i -> String.format("%.1f%s", Math.abs(i >> 2) * multiplier,
                (i & 1) > 0 ? "" : (i & 2) > 0 ? "m" : "k"));
        setValue(1);
    }

    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> rows = ImmutableList.of(
                Component.literal("+k"),//.formatted(Formatting.BOLD),
                Component.literal("+1"),//.formatted(Formatting.BOLD),
                Component.literal("+m"),//.formatted(Formatting.BOLD),
                Component.literal("-k"),//.formatted(Formatting.BOLD),
                Component.literal("-1"),//.formatted(Formatting.BOLD),
                Component.literal("-m")//.formatted(Formatting.BOLD)
        );
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
        return new ValueSettingsBoard(this.label, 250, 20, rows, formatter);
    }

    public void setValueSettings(Player player, ValueSettingsBehaviour.ValueSettings valueSetting, boolean ctrlHeld) {
        int value = Math.max(0, valueSetting.value());
        if (!valueSetting.equals(this.getValueSettings())) {
            this.playFeedbackSound(this);
        }
        int row = valueSetting.row();
        this.setValue(value != 0 ? ((row < 3 ? value : -value) << 2) | row % 3 : 1);
    }

    public ValueSettingsBehaviour.ValueSettings getValueSettings() {
        return new ValueSettingsBehaviour.ValueSettings((this.value < 0 ? 3 : 0) + (this.value & 3), Math.abs(this.value >> 2));
    }

    public MutableComponent formatSettings(ValueSettingsBehaviour.ValueSettings settings) {
        return Lang
                .number(Math.max(0, Math.abs(settings.value() * multiplier)))
                .component();
    }
}
