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

import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

public class ComponentPropertiesWidget extends AbstractSimiWidget {
    private static final Identifier PROPERTIES = PowerGrid.texture("gui/circuit_design_table_properties");

    private final TextRenderer textRenderer;
    @Nullable
    private PlacedComponent component;

    private boolean cursorBlink = true;
    private int tickCount = 0;
    private ComponentProperty<?> selectedProperty = null;
    private String lineBuffer;
    private int cursorPosition;

    public ComponentPropertiesWidget(TextRenderer textRenderer, int x, int y) {
        super(x, y, 150, 150);
        this.textRenderer = textRenderer;
    }

    public void setComponent(@Nullable PlacedComponent component) {
        this.component = component;
        selectedProperty = null;
        lineBuffer = null;
    }

    @Override
    protected void doRender(@NotNull DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        if(component == null)
            return;

        var ms = ctx.getMatrices();
        ms.translate(getX(), getY(), 0);

        ctx.drawTexture(PROPERTIES, 0, 0, 0, 0, getWidth(), 16);
        var key = component.component.getRequiredItem().getTranslationKey();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable(key), getWidth() / 2, 5, 0xFFFFFFFF);

        int yOffset = 16;
        ms.translate(0, yOffset, 0);
        for(var property : component.component.getProperties()) {
            ctx.drawTexture(PROPERTIES, 0, 0, 0, 29, getWidth(), 20);
            ctx.drawText(textRenderer, Text.translatable(property.translationKey()), 6, 6, 0xFF606060, false);
            ctx.enableScissor(getX() + 95, getY() + 2 + yOffset, getX() + 145, getY() + 18 + yOffset);

            if(property == selectedProperty) {
                var text = lineBuffer == null ? component.getString(property) : lineBuffer;
                ctx.drawText(textRenderer, text, 98, 6, -1, false);
                int len = textRenderer.getWidth(text.substring(0, cursorPosition));
                if(cursorBlink)
                    ctx.fill(98 + len, 5, 98 + len + 1, 14, -1);
            } else {
                ctx.drawText(textRenderer, component.getString(property), 98, 6, -1, false);
            }

            ctx.disableScissor();

            yOffset += 20;
            ms.translate(0, 20, 0);
        }

        ctx.drawTexture(PROPERTIES, 0, 0, 0, 50, getWidth(), 6);

        var stack = component.footprint().getRenderedStack();
        if(stack != null) {
            ctx.drawTexture(PROPERTIES, getWidth() / 2 - 8, 4, 240, 0, 16, 10);

            ms.push();
            ms.translate(getWidth() / 2, 14, 0);
            ms.scale(3, 3, 1);
            ms.translate(-8, 0, 0);
            ctx.drawItem(stack, 0, 0);
            ms.pop();
        }
    }

    @Override
    public void tick() {
        if(++tickCount >= 5) {
            cursorBlink = !cursorBlink;
            tickCount = 0;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(component == null)
            return false;
        var x = mouseX - getX();
        var y = mouseY - getY();
        if(selectedProperty != null) {
            acceptInput();
        }
        if(x < 95 || x > 145)
            return false;

        var properties = component.component.getProperties();
        for(int i = 0; i < properties.size(); ++i) {
            int yOffset = 16 + i * 20;
            if(y < yOffset + 2 || y > yOffset + 18)
                continue;

            selectedProperty = properties.get(i);
            lineBuffer = component.getString(selectedProperty);
            cursorPosition = lineBuffer.length();
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(selectedProperty == null)
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

    public void acceptInput() {
        if(selectedProperty == null || component == null)
            return;
        component.setString(selectedProperty, lineBuffer);
        lineBuffer = null;
        selectedProperty = null;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if(selectedProperty == null)
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
