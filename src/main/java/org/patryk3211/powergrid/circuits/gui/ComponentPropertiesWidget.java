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
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import java.util.ArrayList;
import java.util.List;

public class ComponentPropertiesWidget extends AbstractSimiWidget {
    public static final Identifier PROPERTIES = PowerGrid.texture("gui/circuit_design_table_properties");

    private final TextRenderer textRenderer;
    private final int right;
    @Nullable
    private PlacedComponent component;
    private List<PropertyWidget<?, ?>> propertyWidgets = new ArrayList<>();

//    private boolean cursorBlink = true;
//    private int tickCount = 0;
//    private ComponentProperty<?> selectedProperty = null;
//    private String lineBuffer;
//    private int cursorPosition;

    private int propertyCount = 0;

    public ComponentPropertiesWidget(TextRenderer textRenderer, int right, int y) {
        super(right - 150, y, 150, 150);
        this.right = right;
        this.textRenderer = textRenderer;
    }

    public void setComponent(@Nullable PlacedComponent component) {
        this.component = component;
//        selectedProperty = null;
//        lineBuffer = null;

        propertyWidgets.clear();
        if(component != null) {
            var properties = component.component.getProperties();
            int maxTextLength = 0;
            for(var property : properties) {
                var propertyKey = property.translationKey();
                var length = textRenderer.getWidth(Text.translatable(propertyKey));
                if (length > maxTextLength)
                    maxTextLength = length;
            }

            int requiredWidth = maxTextLength + 6 + 60 + 5;
            int width = Math.max(requiredWidth, 120);
            setWidth(width);
            setX(right - width);

            int x = right - 60, y = getY() + 16;
            for(var property : properties) {
                if(property instanceof FloatProperty fProp) {
                    propertyWidgets.add(new FloatPropertyWidget(textRenderer, x, y, component.getEntry(fProp)));
                } else if(property instanceof BooleanProperty bProp) {
                    propertyWidgets.add(new BooleanPropertyWidget(textRenderer, x, y, component.getEntry(bProp)));
                }
                y += 20;
            }
            propertyCount = propertyWidgets.size();
        } else {
            setWidth(120);
            setX(right - 120);
            propertyCount = 0;
        }
    }

    @Override
    protected void doRender(@NotNull DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        if(component == null)
            return;

        var ms = ctx.getMatrices();
        ms.translate(getX(), getY(), 0);

        // Three sliced texture
        int centerSliceSize = Math.max(width - 120, 0);
        ctx.drawTexture(PROPERTIES, 0, 0, 0, 0, 60, 16);
        if(width > 120) {
            ctx.drawRepeatingTexture(PROPERTIES, 60, 0, centerSliceSize, 16, 61, 0, 30, 16);
        }
        ctx.drawTexture(PROPERTIES, 60 + centerSliceSize, 0, 92, 0, 60, 16);

        var key = component.component.getRequiredItem().getTranslationKey();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable(key), getWidth() / 2, 5, 0xFFFFFFFF);

        // Draw common background for each property
        ctx.drawRepeatingTexture(PROPERTIES, 0, 16, 60, propertyCount * 20, 0, 29, 60, 20);
        if(width > 120) {
            ctx.drawRepeatingTexture(PROPERTIES, 60, 16, centerSliceSize, propertyCount * 20, 61, 29, 30, 20);
        }

        ms.push();
        ms.translate(-getX(), -getY(), 0);
        int yOffset = 16;
        for(var widget : propertyWidgets) {
            yOffset = widget.getY() + widget.getHeight() - getY();
            var propertyKey = widget.property.property.translationKey();
            int end = ctx.drawText(textRenderer, Text.translatable(propertyKey), getX() + 6, widget.getY() + 6, 0xFF606060, false);

            widget.render(ctx, mouseX, mouseY, partialTicks);

            var summary = Text.translatableWithFallback(propertyKey + ".summary", "");
            if(!summary.getString().isEmpty()) {
                // Hover description.
                int x1 = getX() + 6;
                int y1 = widget.getY() + 6;
                if(mouseX >= x1 && mouseY >= y1 && mouseX < end && mouseY < y1 + 16)
                    ctx.drawTooltip(textRenderer, summary, mouseX, mouseY);
            }
        }
        ms.pop();

//        ms.translate(0, yOffset, 0);
//        for(var property : component.component.getProperties()) {
////            ctx.drawTexture(PROPERTIES, 0, 0, 0, 29, getWidth(), 20);
//            var propertyKey = property.translationKey();
//            int end = ctx.drawText(textRenderer, Text.translatable(propertyKey), 6, 6, 0xFF606060, false);
//
//            if(property instanceof FloatProperty) {
//                ctx.drawTexture(PROPERTIES, 60 + centerSliceSize, 0, 0, 57, 60, 20);
//                ctx.enableScissor(getX() + 68 + centerSliceSize, getY() + 2 + yOffset, getX() + centerSliceSize + 68 + 46, getY() + 18 + yOffset);
//
//                if(property == selectedProperty) {
//                    var text = lineBuffer == null ? component.getString(property) : lineBuffer;
//                    ctx.drawText(textRenderer, text, 68 + centerSliceSize, 6, -1, false);
//                    int len = textRenderer.getWidth(text.substring(0, cursorPosition));
//                    if(cursorBlink)
//                        ctx.fill(68 + centerSliceSize + len, 5, 68 + centerSliceSize + len + 1, 14, -1);
//                } else {
//                    ctx.drawText(textRenderer, component.getString(property), 68 + centerSliceSize, 6, -1, false);
//                }
//
//                ctx.disableScissor();
//            } else if(property instanceof BooleanProperty) {
//                ctx.drawTexture(PROPERTIES, 60 + centerSliceSize, 0, 0, 78, 60, 20);
//            }
//
//            var summary = Text.translatableWithFallback(propertyKey + ".summary", "");
//            if(!summary.getString().isEmpty()) {
//                // Hover description.
//                int x1 = getX() + 6;
//                int y1 = getY() + 6 + yOffset;
//                if(mouseX >= x1 && mouseY >= y1 && mouseX < x1 + end - 6 && mouseY < y1 + 16)
//                    ctx.drawTooltip(textRenderer, summary, mouseX - getX(), mouseY - getY() - yOffset);
//            }
//
//            yOffset += 20;
//            ms.translate(0, 20, 0);
//        }

        ms.translate(0, yOffset, 0);
        ctx.drawTexture(PROPERTIES, 0, 0, 0, 50, 60, 6);
        if(width > 120) {
            ctx.drawRepeatingTexture(PROPERTIES, 60, 0, centerSliceSize, 6, 61, 50, 30, 6);
        }
        ctx.drawTexture(PROPERTIES, 60 + centerSliceSize, 0, 92, 50, 60, 6);

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
        for(var widget : propertyWidgets) {
            widget.tick();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(component == null)
            return false;

        boolean result = false;
        for(var widget : propertyWidgets) {
            result |= widget.mouseClicked(mouseX, mouseY, button);
        }
//        var x = mouseX - getX();
//        var y = mouseY - getY();
//        if(selectedProperty != null) {
//            acceptInput();
//        }
//        if(x < width - 60 || x > width)
//            return false;
//
//        var properties = component.component.getProperties();
//        for(int i = 0; i < properties.size(); ++i) {
//            int yOffset = 16 + i * 20;
//            if(y < yOffset + 2 || y > yOffset + 18)
//                continue;
//
//            selectedProperty = properties.get(i);
//            if(selectedProperty instanceof BooleanProperty) {
//                selectedProperty = null;
//                return false;
//            }
//            lineBuffer = component.getString(selectedProperty);
//            cursorPosition = lineBuffer.length();
//            return true;
//        }

        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean result = false;
        for(var widget : propertyWidgets) {
            result |= widget.keyPressed(keyCode, scanCode, modifiers);
        }
//        if(selectedProperty == null)
//            return false;
//        switch(scanCode) {
//            case 22 -> {
//                if(cursorPosition > 0) {
//                    if(cursorPosition == lineBuffer.length()) {
//                        lineBuffer = lineBuffer.substring(0, cursorPosition - 1);
//                    } else {
//                        lineBuffer = lineBuffer.substring(0, cursorPosition - 1) + lineBuffer.substring(cursorPosition);
//                    }
//                    --cursorPosition;
//                }
//            }
//            case 113 -> {
//                if(--cursorPosition < 0)
//                    cursorPosition = 0;
//            }
//            case 114 -> {
//                if(++cursorPosition > lineBuffer.length())
//                    cursorPosition = lineBuffer.length();
//            }
//            case 36 -> acceptInput();
//            default -> {
//                return false;
//            }
//        }
        return result;
    }

//    public void acceptInput() {
//        if(selectedProperty == null || component == null)
//            return;
//        component.setString(selectedProperty, lineBuffer);
//        lineBuffer = null;
//        selectedProperty = null;
//    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean result = false;
        for(var widget : propertyWidgets) {
            result |= widget.charTyped(chr, modifiers);
        }
        return result;
//        if(selectedProperty == null)
//            return false;
//
//        if(cursorPosition == 0) {
//            lineBuffer = chr + lineBuffer;
//        } else if(cursorPosition == lineBuffer.length()) {
//            lineBuffer += chr;
//        } else {
//            lineBuffer = lineBuffer.substring(0, cursorPosition) + chr + lineBuffer.substring(cursorPosition);
//        }
//
//        cursorPosition++;
//        return true;
    }
}
