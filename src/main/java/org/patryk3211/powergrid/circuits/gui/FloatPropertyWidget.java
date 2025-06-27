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

public class FloatPropertyWidget extends PropertyWidget<Float, PropertyEntry<Float>> {
    private boolean selected = false;
    private boolean cursorBlink = true;
    private int tickCount = 0;
    private String lineBuffer;
    private int cursorPosition;

    public FloatPropertyWidget(TextRenderer textRenderer, int x, int y, PropertyEntry<Float> property) {
        super(textRenderer, x, y, property);
    }

    @Override
    protected void doRender(@NotNull DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();

        ctx.drawTexture(PROPERTIES, x, y, 0, 57, 60, 20);
        ctx.enableScissor(x + 8, y + 2, x + 8 + 46, y + 18);

        if(selected) {
            var text = lineBuffer == null ? property.stringValue() : lineBuffer;
            ctx.drawText(textRenderer, text, x + 8, y + 6, -1, false);
            int len = textRenderer.getWidth(text.substring(0, cursorPosition));
            if(cursorBlink)
                ctx.fill(x + 8 + len, y + 5, x + 8 + len + 1, y + 14, -1);
        } else {
            ctx.drawText(textRenderer, property.stringValue(), x + 8, y + 6, -1, false);
        }

        ctx.disableScissor();
    }

    public void acceptInput() {
        if(!selected)
            return;
        property.setValue(lineBuffer);
        lineBuffer = null;
        selected = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(mouseX < getX() || mouseY < getY() || mouseX >= getX() + getWidth() || mouseY >= getY() + getHeight()) {
            acceptInput();
            return false;
        } else {
            selected = true;
            lineBuffer = property.stringValue();
            cursorPosition = lineBuffer.length();
            return true;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(!selected)
            return false;
        switch(scanCode) {
            case 22 -> {
                if(cursorPosition > 0) {
                    if(cursorPosition == lineBuffer.length()) {
                        lineBuffer = lineBuffer.substring(0, cursorPosition - 1);
                    } else {
                        lineBuffer = lineBuffer.substring(0, cursorPosition - 1) + lineBuffer.substring(cursorPosition);
                    }
                    --cursorPosition;
                }
            }
            case 113 -> {
                if(--cursorPosition < 0)
                    cursorPosition = 0;
            }
            case 114 -> {
                if(++cursorPosition > lineBuffer.length())
                    cursorPosition = lineBuffer.length();
            }
            case 36 -> acceptInput();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void tick() {
        if(++tickCount >= 5) {
            cursorBlink = !cursorBlink;
            tickCount = 0;
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if(!selected)
            return false;

        if(cursorPosition == 0) {
            lineBuffer = chr + lineBuffer;
        } else if(cursorPosition == lineBuffer.length()) {
            lineBuffer += chr;
        } else {
            lineBuffer = lineBuffer.substring(0, cursorPosition) + chr + lineBuffer.substring(cursorPosition);
        }

        cursorPosition++;
        return true;
    }
}
