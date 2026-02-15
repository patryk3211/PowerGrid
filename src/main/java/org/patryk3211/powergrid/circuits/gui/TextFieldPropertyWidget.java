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

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.properties.PropertyEntry;

import static org.patryk3211.powergrid.circuits.gui.ComponentPropertiesWidget.PROPERTIES;

public class TextFieldPropertyWidget<T, P extends PropertyEntry<T>> extends PropertyWidget<T, P> {
    private final EditBox widget;

    public TextFieldPropertyWidget(Font textRenderer, int x, int y, P property) {
        super(textRenderer, x, y, property);
        widget = new EditBox(textRenderer, x + 8, y + 6, 46, 18, Component.empty());
        widget.setValue(property.stringValue());
        widget.setTextColor(-1);
        widget.setTextColorUneditable(-1);
        widget.setBordered(false);
        widget.setMaxLength(20);
        widget.setEditable(true);
    }

    @Override
    protected void doRender(@NotNull GuiGraphics ctx, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();

        ctx.blit(PROPERTIES, x, y, 0, 57, 60, 20);
        widget.render(ctx, mouseX, mouseY, partialTicks);
    }

    public void acceptInput() {
        property.setValue(widget.getValue());
        widget.setFocused(false);
        widget.setValue(property.stringValue());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return widget.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var clicked = widget.mouseClicked(mouseX, mouseY, button);
        if(widget.isFocused() && !clicked) {
            acceptInput();
        }
        setFocused(clicked);
        return clicked;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(scanCode == 36) {
            acceptInput();
            return true;
        }
        return widget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return widget.charTyped(chr, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        if(!focused)
            acceptInput();
        widget.setFocused(focused);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        return widget.nextFocusPath(navigation);
    }
}
