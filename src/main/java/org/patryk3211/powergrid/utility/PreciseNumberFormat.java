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

import net.minecraft.client.MinecraftClient;

import java.text.NumberFormat;

public class PreciseNumberFormat {
    private static final PreciseNumberFormat instance = new PreciseNumberFormat();

    private NumberFormat format;

    private PreciseNumberFormat() {
        format = NumberFormat.getInstance(MinecraftClient.getInstance().getLanguageManager().getSelectedJavaLocale());
        format.setMaximumFractionDigits(3);
        format.setMinimumFractionDigits(0);
        format.setGroupingUsed(true);
    }

    public static String format(double number) {
        return instance.format.format(number).replace(" ", " ");
    }
}
