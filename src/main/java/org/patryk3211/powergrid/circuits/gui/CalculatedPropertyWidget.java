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
package org.patryk3211.powergrid.circuits.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.properties.PropertyEntry;

import static org.patryk3211.powergrid.circuits.gui.ComponentPropertiesWidget.PROPERTIES;

public class CalculatedPropertyWidget extends PropertyWidget<Object, PropertyEntry.Calculated<Object>> {
    protected CalculatedPropertyWidget(TextRenderer textRenderer, int x, int y, PropertyEntry.Calculated<Object> property) {
        super(textRenderer, x, y, property);
    }

    @Override
    protected void doRender(@NotNull DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();

        ctx.drawTexture(PROPERTIES, x, y, 0, 99, 60, 20);

        var text = property.stringValue();
        int len = textRenderer.getWidth(text);
        ctx.drawText(textRenderer, text, x + 60 - len - 8, y + 6, 0xFF404040, false);
    }
}
