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

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.gui.CircuitEditWidget;
import org.patryk3211.powergrid.circuits.gui.ComponentPropertiesWidget;
import org.patryk3211.powergrid.circuits.schematic.*;
import org.patryk3211.powergrid.collections.ModIcons;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.network.packets.ChangeScreenC2SPacket;
import org.patryk3211.powergrid.network.packets.SaveSchematicC2SPacket;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;
import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.*;
import static org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender.*;

public class CircuitDesignTableEditScreen extends AbstractSimiContainerScreen<CircuitDesignTableEditMenu> {
    private static final Identifier BACKGROUND = PowerGrid.texture("gui/circuit_design_table_edit");
    private static final int WIDTH = 181;
    private static final int HEIGHT = 160;

    public static final int CIRCUIT_SCALE = 8;

    private static final Text TOOLTIP_SAVE = Lang.translateDirect("gui.circuit_designer.save");
    private static final Text TOOLTIP_DISCARD = Lang.translateDirect("gui.circuit_designer.discard");
    private static final Text TOOLTIP_CONNECT = Lang.translateDirect("gui.circuit_designer.connect");
    private static final Text TOOLTIP_DELETE = Lang.translateDirect("gui.circuit_designer.delete");
    private static final Text TOOLTIP_SELECT = Lang.translateDirect("gui.circuit_designer.select");
    private static final Text TOOLTIP_LAYER = Lang.translateDirect("gui.circuit_designer.layer");
    private static final Text TOOLTIP_PLACEABLE = Lang.translate("gui.circuit_designer.placeable")
            .style(Formatting.DARK_GREEN)
            .style(Formatting.ITALIC)
            .component();

    private final CircuitSchematic schematic;
    private List<Line> fgLines;
    private List<Line> bgLines;

    private Tool currentTool = Tool.NONE;
    private Component currentComponent = null;
    private Slot selectedSlot = null;
    private PlacedComponent selectedComponent = null;

    private CircuitEditWidget editWidget;
    private boolean backLayer = false;

    private TextFieldWidget nameField;
    private IconButton acceptBtn;
    private IconButton cancelBtn;

    private IconButton connectBtn;
    private IconButton deleteBtn;
    private IconButton selectBtn;

    private IconButton layerBtn;

    private ComponentPropertiesWidget propertiesWidget;

    public CircuitDesignTableEditScreen(CircuitDesignTableEditMenu container, PlayerInventory inv, Text title) {
        super(container, inv, title);

        // For the editor we take a copy since the underlying circuit can change
        // when packets are received, and we don't want to deal with that here.
        schematic = new CircuitSchematic(container.contentHolder.getSchematic());

        fgLines = schematic.front().calculateLines();
        bgLines = schematic.back().calculateLines();
    }

    private static SoundManager soundManager() {
        return MinecraftClient.getInstance().getSoundManager();
    }

    private static void playSound(AllSoundEvents.SoundEntry sound) {
        soundManager().play(PositionedSoundInstance.master(sound.getMainEvent(), 1.0f));
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT + 4 + PLAYER_INVENTORY.height);
        setWindowOffset(11, 0);

        super.init();

        editWidget = new CircuitEditWidget(schematic, x + 13 - 11, y + 22, GRID_SIZE * CIRCUIT_SCALE, GRID_SIZE * CIRCUIT_SCALE);
        propertiesWidget = new ComponentPropertiesWidget(textRenderer, x - 15, y + 12);

        var name = handler.contentHolder.getSchematicName();
        nameField = new TextFieldWidget(textRenderer, x + 4 - 11, y + 3, 148, 9, Components.immutableEmpty());
        nameField.setText(name);
        nameField.setEditableColor(-1);
        nameField.setUneditableColor(-1);
        nameField.setDrawsBackground(false);
        nameField.setMaxLength(35);
        nameField.setEditable(true);

        currentTool = Tool.NONE;
        selectedComponent = null;
        currentComponent = null;
        selectedSlot = null;

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
            ModdedPackets.getChannel().sendToServer(new SaveSchematicC2SPacket(handler.contentHolder, nameField.getText(), schematic));
            handler.contentHolder.schematic = schematic;
            client.setScreen(new CircuitDesignTableSaveScreen(handler, handler.playerInventory, title));
        });

        cancelBtn.withCallback(() -> ModdedPackets.getChannel().sendToServer(new ChangeScreenC2SPacket(handler.contentHolder, 0)));

        connectBtn.withCallback(toolCallback(this, Tool.CONNECT));
        deleteBtn.withCallback(toolCallback(this, Tool.DELETE));
        selectBtn.withCallback(toolCallback(this, Tool.SELECT));

        layerBtn.withCallback(() -> {
            backLayer = !backLayer;
            layerBtn.setIcon(backLayer ? ModIcons.I_LAYER_BACK : ModIcons.I_LAYER_FRONT);
        });

        editWidget.setSelectionCancelledCallback(() -> currentTool = Tool.NONE);

        addDrawableChild(nameField);
        addDrawableChild(editWidget);
        addDrawableChild(propertiesWidget);
        addDrawableChild(acceptBtn);
        addDrawableChild(cancelBtn);
        addDrawableChild(connectBtn);
        addDrawableChild(deleteBtn);
        addDrawableChild(selectBtn);
        addDrawableChild(layerBtn);
    }

    @Override
    protected List<Text> getTooltipFromItem(ItemStack stack) {
        var lines = super.getTooltipFromItem(stack);
        if(Component.forItem(stack.getItem()) != null) {
            lines.add(TOOLTIP_PLACEABLE);
        }
        return lines;
    }

    private void toolSelect(Tool tool) {
        if(currentComponent != null) {
            currentComponent = null;
            selectedSlot = null;
            editWidget.stopComponentPlacement();
        }
        if(selectedComponent != null) {
            propertiesWidget.setComponent(null);
            selectedComponent = null;
        }
        switch(tool) {
            case CONNECT -> editWidget.requestSelection(CircuitEditWidget.SelectMode.LINE, 0x80FFFFFF, this::placeTrace);
            case DELETE -> editWidget.requestSelection(CircuitEditWidget.SelectMode.AREA, 0x80FF8080, this::deleteArea);
            case SELECT -> editWidget.requestSelection(CircuitEditWidget.SelectMode.POINT, 0x80FFFFFF, this::selectComponent);
        }
    }

    private void toolSelect(Slot slot) {
        var component = Component.forItem(slot.getStack().getItem());
        if(component == null)
            return;
        editWidget.cancelSelection();
        if(selectedComponent != null) {
            selectedComponent = null;
            propertiesWidget.setComponent(null);
        }
        editWidget.componentPlacement(component, this::placeComponent);
        currentComponent = component;
        selectedSlot = slot;
    }

    private CircuitEditWidget.SelectionResult placeTrace(int x1, int y1, int x2, int y2, int clickX, int clickY) {
        var layer = backLayer ? schematic.back() : schematic.front();
        boolean isTrace = layer.get(clickX, clickY);
        layer.fill(x1, y1, x2, y2);

        Line line;
        if(x1 == x2) {
            // Vertical
            line = new Line(true, x1, y1, y2 + 1);
        } else {
            // Horizontal
            line = new Line(false, y1, x1, x2 + 1);
        }
        if(backLayer) {
            bgLines.add(line);
        } else {
            fgLines.add(line);
        }

        playSound(ModdedSoundEvents.UI_PLACE_TRACE);
        if(schematic.isPad(clickX, clickY) || isTrace)
            return CircuitEditWidget.SelectionResult.BEGIN_NEW;
        return CircuitEditWidget.SelectionResult.CONTINUE;
    }

    public CircuitEditWidget.SelectionResult deleteArea(int x1, int y1, int x2, int y2, int clickX, int clickY) {
        var layer = backLayer ? schematic.back() : schematic.front();
        layer.clear(x1, y1, x2, y2);
        if(backLayer) {
            bgLines = layer.calculateLines();
        } else {
            fgLines = layer.calculateLines();
        }
        schematic.removeComponents(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
        playSound(ModdedSoundEvents.UI_DELETE_AREA);
        return CircuitEditWidget.SelectionResult.BEGIN_NEW;
    }

    public CircuitEditWidget.SelectionResult selectComponent(int x1, int y1, int x2, int y2, int clickX, int clickY) {
        var placed = schematic.getComponent(x1 / GRID_TO_GRID_SCALE, y1 / GRID_TO_GRID_SCALE);
        if(placed == null)
            return CircuitEditWidget.SelectionResult.IGNORE;

        selectedComponent = placed;
        propertiesWidget.setComponent(selectedComponent);
        currentTool = Tool.NONE;
        playSound(ModdedSoundEvents.UI_SELECT_COMPONENT);
        return CircuitEditWidget.SelectionResult.END;
    }

    public void placeComponent(int x, int y) {
        if(schematic.canPlace(currentComponent, x, y)) {
            playSound(ModdedSoundEvents.UI_PLACE_COMPONENT);
            schematic.placeComponent(currentComponent, x, y);
        } else {
            playSound(ModdedSoundEvents.UI_PLACE_FAIL);
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int bgX = getLeftOfCentered(WIDTH);
        int invY = y + HEIGHT + 4;
        renderPlayerInventory(ctx, bgX + WIDTH - PLAYER_INVENTORY.width, invY);

        for(int k = 0; k < this.handler.slots.size(); ++k) {
            var slot = this.handler.slots.get(k);
            if(!slot.isEnabled() || !slot.hasStack())
                continue;
            if(slot == selectedSlot) {
                ctx.drawTexture(BACKGROUND, x + slot.x - 1, y + slot.y - 1 , 232, 18, 18, 18);
                continue;
            }
            if(Component.forItem(slot.getStack().getItem()) == null)
                continue;
            ctx.drawTexture(BACKGROUND, x + slot.x - 1, y + slot.y - 1 , 232, 0, 18, 18);
        }

        ctx.drawTexture(BACKGROUND, bgX, y, 0, 0, WIDTH, HEIGHT);

        int bpX = bgX + 13, bpY = y + 22;
        if(!backLayer) {
            CircuitSchematicRender.renderLayer(bgLines, ctx, bpX, bpY, CIRCUIT_SCALE, COLOR_TRACE_BACK);
            CircuitSchematicRender.renderLayer(fgLines, ctx, bpX, bpY, CIRCUIT_SCALE, COLOR_TRACE_FRONT);
        } else {
            CircuitSchematicRender.renderLayer(fgLines, ctx, bpX, bpY, CIRCUIT_SCALE, COLOR_TRACE_BACK);
            CircuitSchematicRender.renderLayer(bgLines, ctx, bpX, bpY, CIRCUIT_SCALE, COLOR_TRACE_FRONT);
        }

        CircuitSchematicRender.renderComponents(schematic, ctx, bpX, bpY, CIRCUIT_SCALE);

        if(currentTool.y > 0) {
            ctx.drawTexture(BACKGROUND, x + 172 - 11, y + currentTool.y, 250, 0, 6, 18);
        }

        if(selectedComponent != null) {
            var footprint = selectedComponent.footprint();
            ctx.drawBorder(
                    bpX + selectedComponent.x * CIRCUIT_SCALE * GRID_TO_GRID_SCALE - 1, bpY + selectedComponent.y * CIRCUIT_SCALE * GRID_TO_GRID_SCALE - 1,
                    footprint.getWidth() * CIRCUIT_SCALE * GRID_TO_GRID_SCALE + 2, footprint.getHeight() * CIRCUIT_SCALE * GRID_TO_GRID_SCALE + 2,
                    COLOR_SELECT_OUTLINE
            );
        }
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = editWidget.getX(), y = editWidget.getY();
        int gridX = (mouseX - x) / CIRCUIT_SCALE;
        int gridY = (mouseY - y) / CIRCUIT_SCALE;

        for(var placed : schematic.components()) {
            var localX = gridX - placed.x * GRID_TO_GRID_SCALE;
            var localY = gridY - placed.y * GRID_TO_GRID_SCALE;
            if(localX < 0 || localY < 0)
                continue;
            var footprint = placed.footprint();
            if(localX >= footprint.getWidth() * GRID_TO_GRID_SCALE || localY >= footprint.getHeight() * GRID_TO_GRID_SCALE)
                continue;
            var tooltip = footprint.getTooltip(localX, localY);
            if(tooltip == null)
                continue;
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // TODO: Add key binds for tools
        // Prevent closing on Inventory Key and ESC
        var focused = getFocused();
        return focused != null && focused.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        if(slot == null || !slot.hasStack() || actionType != SlotActionType.PICKUP)
            return;
        toolSelect(slot);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(button == 1 && currentComponent != null) {
            currentComponent = null;
            selectedSlot = null;
            editWidget.stopComponentPlacement();
            if(selectedComponent != null) {
                selectedComponent = null;
                propertiesWidget.setComponent(null);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static Runnable toolCallback(CircuitDesignTableEditScreen screen, Tool tool) {
        return () -> {
            screen.currentTool = tool;
            screen.toolSelect(tool);
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
