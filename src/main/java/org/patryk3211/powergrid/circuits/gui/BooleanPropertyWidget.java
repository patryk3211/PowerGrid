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

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.properties.PropertyEntry;
import org.patryk3211.powergrid.collections.ModIcons;

import static org.patryk3211.powergrid.circuits.gui.ComponentPropertiesWidget.PROPERTIES;

public class BooleanPropertyWidget extends PropertyWidget<Boolean, PropertyEntry<Boolean>> {
    protected BooleanPropertyWidget(TextRenderer textRenderer, int x, int y, PropertyEntry<Boolean> property) {
        super(textRenderer, x, y, property);
    }

    @Override
    protected void doRender(@NotNull DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();

        ctx.drawTexture(PROPERTIES, x, y, 0, 78, 60, 20);
        if(property.get()) {
            ctx.drawTexture(PROPERTIES, x + 48, y + 1, 60, 79, 6, 18);
        }

        hovered = mouseX >= x + 29 && mouseY >= y + 1 && mouseX < x + 29 + 18 && mouseY < y + 1 + 18;

        var bg = hovered ? AllGuiTextures.BUTTON_HOVER : AllGuiTextures.BUTTON;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        ctx.drawTexture(bg.location, x + 29, y + 1, bg.startX, bg.startY, bg.width, bg.height);
        ModIcons.I_TOGGLE.render(ctx, x + 30, y + 2);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = getX();
        int y = getY();
        if(mouseX >= x + 29 && mouseY >= y + 1 && mouseX < x + 29 + 18 && mouseY < y + 1 + 18) {
            property.set(!property.get());
            return super.mouseClicked(mouseX, mouseY, button);
        } else {
            return false;
        }
    }
}
