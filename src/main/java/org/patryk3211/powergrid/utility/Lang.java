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

import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.LangBuilder;
import net.minecraft.text.MutableText;
import org.patryk3211.powergrid.PowerGrid;

public class Lang extends com.simibubi.create.foundation.utility.Lang {
    public static MutableText translateDirect(String key, Object... args) {
        return Components.translatable(PowerGrid.MOD_ID + "." + key, resolveBuilders(args));
    }

    public static LangBuilder builder() {
        return com.simibubi.create.foundation.utility.Lang.builder(PowerGrid.MOD_ID);
    }

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }
}
