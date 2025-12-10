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
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.GuiGraphics;

public class PunchCardButton extends AbstractSimiWidget {
    private final byte[] dataRef;
    private final int row, column;

    public PunchCardButton(int x, int y, int r, int c, byte[] dataRef) {
        super(x, y, 10, 7);
        this.row = r;
        this.column = c;
        this.dataRef = dataRef;
    }

    public boolean getState() {
        return (dataRef[column] & (1 << row)) != 0;
    }

    public void flipState() {
        dataRef[column] ^= (byte) (1 << row);
    }

    @Override
    public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if(visible) {
            isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if(!getState()) {
                if (isHovered) {
                    graphics.blit(PunchCardScreen.BACKGROUND, getX(), getY(), 240, 20, 10, 7);
                } else {
                    graphics.blit(PunchCardScreen.BACKGROUND, getX(), getY(), 228, 20, 10, 7);
                }
            } else {
                if (isHovered) {
                    graphics.blit(PunchCardScreen.BACKGROUND, getX(), getY(), 240, 29, 10, 7);
                } else {
                    graphics.blit(PunchCardScreen.BACKGROUND, getX(), getY(), 228, 29, 10, 7);
                }
            }
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        flipState();
        if(getState())
            runCallback(mouseX, mouseY);
    }
}
