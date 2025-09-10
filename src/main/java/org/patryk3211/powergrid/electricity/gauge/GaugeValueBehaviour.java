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
package org.patryk3211.powergrid.electricity.gauge;

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

// 20mA, 200mA, 2A, 20A
// 2V, 20V, 200V, 2000V
public class GaugeValueBehaviour extends ScrollValueBehaviour {
    private final Component unit;
    private final float[] values;

    public GaugeValueBehaviour(Component label, Component unit, float[] values, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        this.unit = unit;
        this.values = values;
        between(0, values.length - 1);
        withFormatter(this::format);
    }

    private String format(int value) {
        return NumberFormats.formatPrecise(values[value]) + " " + unit.getString();
    }

    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> rows = ImmutableList.of(unit);
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
        return new ValueSettingsBoard(this.label, max, 1, rows, formatter);
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return Lang
                .number(values[settings.value()])
                .add(Component.literal(" "))
                .add(unit)
                .component();
    }
}
