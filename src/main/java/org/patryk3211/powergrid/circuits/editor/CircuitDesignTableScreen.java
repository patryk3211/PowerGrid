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

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.gui.CircuitEditButton;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender;
import org.patryk3211.powergrid.circuits.schematic.Line;
import org.patryk3211.powergrid.collections.ModIcons;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.ChangeScreenC2SPacket;
import org.patryk3211.powergrid.network.packets.SaveSchematicC2SPacket;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;
import static org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender.*;

public class CircuitDesignTableScreen extends AbstractSimiContainerScreen<CircuitDesignTableMenu> {
    private static final Identifier BACKGROUND = PowerGrid.texture("gui/circuit_design_table");
    private static final int WIDTH = 180;
    private static final int HEIGHT = 92;
    public static final int SCALE = 4;

    private static final Text TOOLTIP_EDIT = Lang.translateDirect("gui.circuit_designer.edit");

    private IconButton confirmButton;
    private IconButton loadButton;
    private CircuitEditButton editButton;

    private final CircuitSchematic schematic;
    private List<Line> linesFg;
    private List<Line> linesBg;

    public CircuitDesignTableScreen(CircuitDesignTableMenu container, PlayerInventory inv, Text title) {
        super(container, inv, title);

        schematic = container.contentHolder.getSchematic();
        linesFg = schematic.front().calculateLines();
        linesBg = schematic.back().calculateLines();
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT + 4 + PLAYER_INVENTORY.height);
        setWindowOffset(11, 0);

        super.init();

        confirmButton = new IconButton(x + 116 - 11, y + 66, AllIcons.I_CONFIRM);
        loadButton = new IconButton(x + 15 - 11, y + 65, ModIcons.I_RIGHT);
        editButton = new CircuitEditButton(x + 42 - 11, y + 18, 68, 68);

        confirmButton.withCallback(() -> {
            ModdedPackets.getChannel().sendToServer(new SaveSchematicC2SPacket(handler.contentHolder, false));
        });

        loadButton.withCallback(() -> {
            ModdedPackets.getChannel().sendToServer(new SaveSchematicC2SPacket(handler.contentHolder, true));
        });

        editButton.setTooltip(Tooltip.of(TOOLTIP_EDIT));
        editButton.withCallback(() -> {
            ModdedPackets.getChannel().sendToServer(new ChangeScreenC2SPacket(handler.contentHolder, 1));
        });

        addDrawableChild(confirmButton);
        addDrawableChild(loadButton);
        addDrawableChild(editButton);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int bgX = getLeftOfCentered(WIDTH);
        int invY = y + HEIGHT + 4;
        renderPlayerInventory(ctx, bgX + 2, invY);

        ctx.drawTexture(BACKGROUND, bgX, y, 0, 0, WIDTH, HEIGHT);

        if(handler.contentHolder.schematicChanged) {
            linesFg = schematic.front().calculateLines();
            linesBg = schematic.back().calculateLines();
            handler.contentHolder.schematicChanged = false;
        }

        CircuitSchematicRender.renderLayer(linesFg, ctx, x + 44 - 11, y + 20, SCALE, COLOR_TRACE_FRONT);
        CircuitSchematicRender.renderLayer(linesBg, ctx, x + 44 - 11, y + 20, SCALE, COLOR_TRACE_BACK);
        CircuitSchematicRender.renderComponents(schematic, ctx, x + 44 - 11, y + 20, SCALE);

        ctx.drawCenteredTextWithShadow(textRenderer, title, x + (WIDTH - 8) / 2, y + 3, 0xFFFFFF);
    }
}
