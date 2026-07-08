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

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.network.chat.Component;

public enum Unit {
    VOLTAGE("generic.unit.volt"),
    CURRENT("generic.unit.amp"),
    POWER("generic.unit.watt"),
    RESISTANCE("generic.unit.ohm"),
    TEMPERATURE("generic.unit.temperature"),
    ENERGY("generic.unit.energy");

    private final String translationKey;

    Unit(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public LangBuilder get() {
        return Lang.unit(this);
    }

    public Component getName() {
        return Lang.translateDirect(translationKey + ".name");
    }

    public String string() {
        return get().string();
    }

    public LangBuilder formatWithPrefixes(double value) {
        LangBuilder valueText = Lang.builder();
        if(Math.abs(value) < 1) {
            // Millis
            valueText.add(Lang.number(value * 1000))
                    .add(Component.nullToEmpty(" m"))
                    .add(get());
        } else if(Math.abs(value) < 1000) {
            valueText.add(Lang.number(value))
                    .add(Component.nullToEmpty(" "))
                    .add(get());
        } else if(Math.abs(value) < 1000000) {
            valueText.add(Lang.number(value / 1000))
                    .add(Component.nullToEmpty(" k"))
                    .add(get());
        } else {
            valueText.add(Lang.number(value / 1000000))
                    .add(Component.nullToEmpty(" M"))
                    .add(get());
        }
        return valueText;
    }

    public Component format(double v) {
        return Lang.number(v)
                .add(Component.literal(" "))
                .add(get())
                .component();
    }
}
