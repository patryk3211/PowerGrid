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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.utility.Lang;

public interface IDecoratedTerminal {
    Component POSITIVE = Lang.builder()
            .translate("generic.positive_terminal")
            .style(ChatFormatting.RED)
            .component();
    Component NEGATIVE = Lang.builder()
            .translate("generic.negative_terminal")
            .style(ChatFormatting.BLUE)
            .component();
    Component COMMON = Lang.builder()
            .translate("generic.common")
            .style(ChatFormatting.BLUE)
            .component();
    Component CONNECTOR = Lang.builder()
            .translate("generic.terminal")
            .style(ChatFormatting.GRAY)
            .component();
    Component CONTROL = Lang.builder()
            .translate("generic.control_terminal")
            .style(ChatFormatting.DARK_GREEN)
            .component();
    Component TAP = Lang.builder()
            .translate("generic.tap")
            .style(ChatFormatting.GRAY)
            .component();
    Component SOCKET = Lang.builder()
            .translate("generic.socket")
            .style(ChatFormatting.YELLOW)
            .component();
    Component CORD = Lang.builder()
            .translate("generic.cord")
            .style(ChatFormatting.GRAY)
            .component();

    int RED = 0xFF3B3B;
    int BLUE = 0x3B80FF;
    int GRAY = 0xAAAAAA;
    int GREEN = 0x00AA00;

    @Nullable
    Component getName();
    AABB getOutline();

    default int getColor() {
        return GRAY;
    }
}
