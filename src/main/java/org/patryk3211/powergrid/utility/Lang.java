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
import net.minecraft.network.chat.MutableComponent;
import org.patryk3211.powergrid.PowerGrid;

public class Lang extends net.createmod.catnip.lang.Lang {
    public static MutableComponent translateDirect(String key, Object... args) {
        return builder().translate(key, args).component();
    }

    public static LangBuilder builder() {
        return Lang.builder(PowerGrid.MOD_ID);
    }

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }

    public static LangBuilder unit(String unit) {
        return builder().translate("generic.unit." + unit);
    }

    public static LangBuilder unit(Unit unit) {
        return builder().translate(unit.getTranslationKey());
    }

    public static LangBuilder text(String literal) {
        return builder().text(literal);
    }

    public static LangBuilder number(double n) {
        return builder().text(PreciseNumberFormat.format(n));
    }
}
