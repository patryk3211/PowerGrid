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
package org.patryk3211.powergrid.circuits.editor;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.patryk3211.powergrid.utility.Lang;

public class CircuitDesignTableSaveScreen extends HandledScreen<CircuitDesignTableEditMenu> {
    private static final Text TEXT_SAVING = Lang.translateDirect("gui.circuit_builder.saving");

    public CircuitDesignTableSaveScreen(CircuitDesignTableEditMenu container, PlayerInventory inv, Text title) {
        super(container, inv, title);
    }

    @Override
    protected void init() {
        backgroundWidth = 0;
        backgroundHeight = 0;
        super.init();
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        ctx.fillGradient(0, 0, width, height, -1072689136, -804253680);

        drawBackground(ctx, partialTicks, mouseX, mouseY);

        ctx.drawCenteredTextWithShadow(textRenderer, TEXT_SAVING, x, y, 0xFFFFFFFF);

        drawForeground(ctx, mouseX, mouseY);
    }
}
