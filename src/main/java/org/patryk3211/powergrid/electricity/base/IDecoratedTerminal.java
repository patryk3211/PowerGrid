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
package org.patryk3211.powergrid.electricity.base;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.utility.Lang;

public interface IDecoratedTerminal {
    Text POSITIVE = Lang.builder()
            .translate("generic.positive_terminal")
            .style(Formatting.RED)
            .component();
    Text NEGATIVE = Lang.builder()
            .translate("generic.negative_terminal")
            .style(Formatting.BLUE)
            .component();
    Text CONNECTOR = Lang.builder()
            .translate("generic.terminal")
            .style(Formatting.GRAY)
            .component();
    Text CONTROL = Lang.builder()
            .translate("generic.control_terminal")
            .style(Formatting.DARK_GREEN)
            .component();

    int RED = 0xFF3B3B;
    int BLUE = 0x3B80FF;
    int GRAY = 0xAAAAAA;
    int GREEN = 0x00AA00;

    @Nullable
    Text getName();
    Box getOutline();

    default int getColor() {
        return GRAY;
    }
}
