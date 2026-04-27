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

import java.text.NumberFormat;

public class NumberFormats {
    private static final NumberFormats instance = new NumberFormats();

    private final NumberFormat precise;
    private final NumberFormat constant;

    private NumberFormats() {
        precise = NumberFormat.getInstance();
        precise.setMaximumFractionDigits(3);
        precise.setMinimumFractionDigits(0);
        precise.setGroupingUsed(true);

        constant = NumberFormat.getInstance();
        constant.setMaximumFractionDigits(1);
        constant.setMinimumFractionDigits(1);
        constant.setGroupingUsed(true);
    }

    public static String formatPrecise(double number) {
        return instance.precise.format(number).replace(" ", " ");
    }

    public static String formatConstant(double number) {
        return instance.constant.format(number).replace(" ", " ");
    }
}
