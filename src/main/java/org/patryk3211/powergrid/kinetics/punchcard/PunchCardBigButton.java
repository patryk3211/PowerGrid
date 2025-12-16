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
package org.patryk3211.powergrid.kinetics.punchcard;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllKeys;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

public class PunchCardBigButton extends AbstractSimiWidget {
    private final int u, v;

    public PunchCardBigButton(int x, int y, int u, int v) {
        super(x, y, 18, 18);
        this.u = u;
        this.v = v;
    }

    @Override
    public void doRender(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if(isHovered && AllKeys.isMouseButtonDown(0)) {
                graphics.blit(PunchCardScreen.BACKGROUND, getX(), getY(), u, v, 18, 18);
            } else if(isHovered) {
                graphics.blit(PunchCardScreen.BACKGROUND, getX(), getY(), u, v + 38, 18, 18);
            } else {
                graphics.blit(PunchCardScreen.BACKGROUND, getX(), getY(), u, v + 19, 18, 18);
            }
        }
    }
}
