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
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;

public class CircuitEditButton extends AbstractSimiWidget {
    public CircuitEditButton(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    @Override
    public void doRender(@NotNull DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        if(visible) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if(isMouseOver(mouseX, mouseY)) {
                ctx.drawHorizontalLine(getX() - 1, getX() + width, getY() - 1, 0xFFFFFFFF);
                ctx.drawVerticalLine(getX() - 1, getY() - 1, getY() + height + 1, 0xFFFFFFFF);
                ctx.drawHorizontalLine(getX(), getX() + width, getY() + height, 0xFF888888);
                ctx.drawVerticalLine(getX() + width, getY() - 1, getY() + height + 1, 0xFF888888);
//                ctx.drawBorder(getX(), getY(), width + 2, height + 2, 0xFF888888);
            }
        }
    }
}
