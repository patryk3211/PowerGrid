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
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.gui.CircuitEditWidget;
import org.patryk3211.powergrid.circuits.schematic.CircuitLayer;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender;
import org.patryk3211.powergrid.collections.ModIcons;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.ChangeScreenC2SPacket;
import org.patryk3211.powergrid.network.packets.SaveSchematicC2SPacket;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

public class CircuitDesignTableEditScreen extends AbstractSimiContainerScreen<CircuitDesignTableEditMenu> {
    private static final Identifier BACKGROUND = PowerGrid.texture("gui/circuit_design_table_edit");
    private static final int WIDTH = 181;
    private static final int HEIGHT = 160;

    private static final Text TOOLTIP_SAVE = Lang.translateDirect("gui.circuit_designer.save");
    private static final Text TOOLTIP_DISCARD = Lang.translateDirect("gui.circuit_designer.discard");
    private static final Text TOOLTIP_CONNECT = Lang.translateDirect("gui.circuit_designer.connect");
    private static final Text TOOLTIP_DELETE = Lang.translateDirect("gui.circuit_designer.delete");
    private static final Text TOOLTIP_SELECT = Lang.translateDirect("gui.circuit_designer.select");
    private static final Text TOOLTIP_LAYER = Lang.translateDirect("gui.circuit_designer.layer");

    private final CircuitSchematic schematic;
    private List<CircuitLayer.Line> fgLines;
    private List<CircuitLayer.Line> bgLines;

    private Tool currentTool = Tool.NONE;

    private CircuitEditWidget editWidget;
    private boolean backLayer = false;

    private IconButton acceptBtn;
    private IconButton cancelBtn;

    private IconButton connectBtn;
    private IconButton deleteBtn;
    private IconButton selectBtn;

    private IconButton layerBtn;

    public CircuitDesignTableEditScreen(CircuitDesignTableEditMenu container, PlayerInventory inv, Text title) {
        super(container, inv, title);

        // For the editor we take a copy since the underlying circuit can change
        // when packets are received, and we don't want to deal with that here.
        schematic = new CircuitSchematic(container.contentHolder.getSchematic());

        fgLines = schematic.front().calculateLines();
        bgLines = schematic.back().calculateLines();
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT + 4 + PLAYER_INVENTORY.height);
        setWindowOffset(11, 0);

        super.init();

        editWidget = new CircuitEditWidget(x + 13 - 11, y + 22, 32 * 4, 32 * 4);

        acceptBtn = new IconButton(x + 153 - 11, y + 15, AllIcons.I_CONFIRM);
        cancelBtn = new IconButton(x + 153 - 11, y + 35, ModIcons.I_CANCEL);
        connectBtn = new IconButton(x + 153 - 11, y + 55, ModIcons.I_CONNECT);
        deleteBtn = new IconButton(x + 153 - 11, y + 73, AllIcons.I_TRASH);
        selectBtn = new IconButton(x + 153 - 11, y + 91, AllIcons.I_TARGET);
        layerBtn = new IconButton(x + 153 - 11, y + 111, ModIcons.I_LAYER_FRONT);

        acceptBtn.setToolTip(TOOLTIP_SAVE);
        cancelBtn.setToolTip(TOOLTIP_DISCARD);
        connectBtn.setToolTip(TOOLTIP_CONNECT);
        deleteBtn.setToolTip(TOOLTIP_DELETE);
        selectBtn.setToolTip(TOOLTIP_SELECT);
        layerBtn.setToolTip(TOOLTIP_LAYER);

        acceptBtn.withCallback(() -> {
            ModdedPackets.getChannel().sendToServer(new SaveSchematicC2SPacket(handler.contentHolder, schematic));
            handler.contentHolder.schematic = schematic;
            client.setScreen(new CircuitDesignTableSaveScreen(handler, handler.playerInventory, title));
        });

        cancelBtn.withCallback(() -> {
            ModdedPackets.getChannel().sendToServer(new ChangeScreenC2SPacket(handler.contentHolder, 0));
        });

        connectBtn.withCallback(toolCallback(this, Tool.CONNECT));
        deleteBtn.withCallback(toolCallback(this, Tool.DELETE));
        selectBtn.withCallback(toolCallback(this, Tool.SELECT));

        layerBtn.withCallback(() -> {
            backLayer = !backLayer;
            layerBtn.setIcon(backLayer ? ModIcons.I_LAYER_BACK : ModIcons.I_LAYER_FRONT);
        });

        editWidget.setSelectionCanceledCallback(() -> currentTool = Tool.NONE);

        addDrawableChild(editWidget);
        addDrawableChild(acceptBtn);
        addDrawableChild(cancelBtn);
        addDrawableChild(connectBtn);
        addDrawableChild(deleteBtn);
        addDrawableChild(selectBtn);
        addDrawableChild(layerBtn);
    }

    private void toolSelected(Tool tool) {
        switch(tool) {
            case CONNECT -> {
                editWidget.requestSelection(CircuitEditWidget.SelectMode.LINE, 0x80FFFFFF, this::placeTrace);
            }
            case DELETE -> {
                editWidget.requestSelection(CircuitEditWidget.SelectMode.AREA, 0x80FF8080, this::deleteArea);
            }
        }
    }

    private CircuitEditWidget.SelectionResult placeTrace(int x1, int y1, int x2, int y2) {
        var layer = backLayer ? schematic.back() : schematic.front();
        layer.fill(x1, y1, x2, y2);

        CircuitLayer.Line line;
        if(x1 == x2) {
            // Vertical
            line = new CircuitLayer.Line(true, x1, y1, y2 + 1);
        } else {
            // Horizontal
            line = new CircuitLayer.Line(false, y1, x1, x2 + 1);
        }
        if(backLayer) {
            bgLines.add(line);
        } else {
            fgLines.add(line);
        }
        return CircuitEditWidget.SelectionResult.CONTINUE;
    }

    public CircuitEditWidget.SelectionResult deleteArea(int x1, int y1, int x2, int y2) {
        var layer = backLayer ? schematic.back() : schematic.front();
        layer.clear(x1, y1, x2, y2);
        if(backLayer) {
            bgLines = layer.calculateLines();
        } else {
            fgLines = layer.calculateLines();
        }
        return CircuitEditWidget.SelectionResult.CONTINUE;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int bgX = getLeftOfCentered(WIDTH);
        int invY = y + HEIGHT + 4;
        renderPlayerInventory(ctx, bgX + WIDTH - PLAYER_INVENTORY.width, invY);

        ctx.drawTexture(BACKGROUND, bgX, y, 0, 0, WIDTH, HEIGHT);

        if(!backLayer) {
            CircuitSchematicRender.renderLayer(bgLines, ctx, bgX + 13, y + 22, 4, 0x80FFFFFF);
            CircuitSchematicRender.renderLayer(fgLines, ctx, bgX + 13, y + 22, 4, 0xFFFFFFFF);
        } else {
            CircuitSchematicRender.renderLayer(fgLines, ctx, bgX + 13, y + 22, 4, 0x80FFFFFF);
            CircuitSchematicRender.renderLayer(bgLines, ctx, bgX + 13, y + 22, 4, 0xFFFFFFFF);
        }

        if(currentTool != Tool.NONE) {
            ctx.drawTexture(BACKGROUND, x + 172 - 11, y + currentTool.y, 250, 0, 6, 18);
        }
    }

    private static Runnable toolCallback(CircuitDesignTableEditScreen screen, Tool tool) {
        return () -> {
            screen.currentTool = tool;
            screen.toolSelected(tool);
        };
    }

    private enum Tool {
        NONE(0),
        CONNECT(55),
        DELETE(73),
        SELECT(91);

        public final int y;

        Tool(int y) {
            this.y = y;
        }
    }
}
