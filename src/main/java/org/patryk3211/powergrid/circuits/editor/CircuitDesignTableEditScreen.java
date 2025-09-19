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

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.gui.CircuitEditWidget;
import org.patryk3211.powergrid.circuits.gui.ComponentPropertiesWidget;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender;
import org.patryk3211.powergrid.circuits.schematic.Line;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.collections.ModIcons;
import org.patryk3211.powergrid.collections.ModdedKeys;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.network.packets.ChangeScreenC2SPacket;
import org.patryk3211.powergrid.network.packets.SaveSchematicC2SPacket;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;
import java.util.UUID;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;
import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_SIZE;
import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_TO_GRID_SCALE;
import static org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender.*;

public class CircuitDesignTableEditScreen<T extends CircuitEditMenu<?>> extends AbstractSimiContainerScreen<T> {
    private static final ResourceLocation BACKGROUND = PowerGrid.texture("gui/circuit_design_table_edit");
    private static final int WIDTH = 182;
    private static final int HEIGHT = 160;

    public static final int CIRCUIT_SCALE = 8;

    private static final net.minecraft.network.chat.Component TOOLTIP_SAVE = Lang.translateDirect("gui.circuit_designer.save");
    private static final net.minecraft.network.chat.Component TOOLTIP_DISCARD = Lang.translateDirect("gui.circuit_designer.discard");
    private static final net.minecraft.network.chat.Component TOOLTIP_CONNECT = Lang.translateDirect("gui.circuit_designer.connect");
    private static final net.minecraft.network.chat.Component TOOLTIP_DELETE = Lang.translateDirect("gui.circuit_designer.delete");
    private static final net.minecraft.network.chat.Component TOOLTIP_SELECT = Lang.translateDirect("gui.circuit_designer.select");
    private static final net.minecraft.network.chat.Component TOOLTIP_LAYER = Lang.translateDirect("gui.circuit_designer.layer");
    private static final net.minecraft.network.chat.Component TOOLTIP_PLACEABLE = Lang.translate("gui.circuit_designer.placeable")
            .style(ChatFormatting.DARK_GREEN)
            .style(ChatFormatting.ITALIC)
            .component();
    private static final net.minecraft.network.chat.Component TEXT_SAVING = Lang.translateDirect("gui.circuit_designer.saving");
    private static final net.minecraft.network.chat.Component TEXT_NOT_SAVED = Lang.translateDirect("gui.circuit_designer.not_saved");

    private final CircuitSchematic schematic;
    private List<Line> fgLines;
    private List<Line> bgLines;

    private Tool currentTool = Tool.SELECT;
    private PlacedComponent currentComponent = null;
    private Slot selectedSlot = null;
    private PlacedComponent selectedComponent = null;

    private CircuitEditWidget editWidget;
    private boolean backLayer = false;

    private EditBox nameField;
    private IconButton acceptBtn;
    private IconButton cancelBtn;

    private IconButton connectBtn;
    private IconButton deleteBtn;
    private IconButton selectBtn;

    private IconButton layerBtn;

    private ComponentPropertiesWidget propertiesWidget;
    private boolean changed = false;

    private boolean saving = false;
    private int unsavedPopupTimeout = 0;

    public CircuitDesignTableEditScreen(T container, Inventory inv, net.minecraft.network.chat.Component title) {
        super(container, inv, title);

        // For the editor we take a copy since the underlying circuit can change
        // when packets are received, and we don't want to deal with that here.
        schematic = new CircuitSchematic(container.contentHolder.getSchematic());

        fgLines = schematic.front().calculateLines();
        bgLines = schematic.back().calculateLines();
    }

    private static SoundManager soundManager() {
        return Minecraft.getInstance().getSoundManager();
    }

    private static void playSound(AllSoundEvents.SoundEntry sound) {
        soundManager().play(SimpleSoundInstance.forUI(sound.getMainEvent(), 1.0f));
    }

    public void save() {
        ModdedPackets.getChannel().sendToServer(new SaveSchematicC2SPacket(menu.contentHolder, nameField.getValue(), schematic));
        if(menu.contentHolder instanceof CircuitDesignTableBlockEntity table) {
            menu.contentHolder.setSchematic(schematic);
            saving = true;
        } else {
            onClose();
        }
    }

    public void discard() {
        if(menu.contentHolder instanceof CircuitDesignTableBlockEntity table) {
            ModdedPackets.getChannel().sendToServer(new ChangeScreenC2SPacket(table, 0));
        } else {
            onClose();
        }
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT + 4 + PLAYER_INVENTORY.getHeight());
        setWindowOffset(11, 0);

        super.init();

        editWidget = new CircuitEditWidget(font, schematic, leftPos + 13 - 11, topPos + 22, GRID_SIZE * CIRCUIT_SCALE, GRID_SIZE * CIRCUIT_SCALE);
        propertiesWidget = new ComponentPropertiesWidget(font, leftPos - 15, topPos + 12);

        var name = menu.contentHolder.getSchematicName();
        nameField = new EditBox(font, leftPos + 4 - 11, topPos + 3, 148, 9, net.minecraft.network.chat.Component.empty());
        nameField.setValue(name);
        nameField.setTextColor(-1);
        nameField.setTextColorUneditable(-1);
        nameField.setBordered(false);
        nameField.setMaxLength(35);
        nameField.setEditable(true);

        selectedComponent = null;
        currentComponent = null;
        selectedSlot = null;

        final var BUTTONS_X = leftPos + 154 - 11;
        acceptBtn = new IconButton(BUTTONS_X, topPos + 43, AllIcons.I_CONFIRM);
        cancelBtn = new IconButton(BUTTONS_X, topPos + 63, ModIcons.I_CANCEL);
        connectBtn = new IconButton(BUTTONS_X, topPos + 83, ModIcons.I_CONNECT);
        deleteBtn = new IconButton(BUTTONS_X, topPos + 101, AllIcons.I_TRASH);
        selectBtn = new IconButton(BUTTONS_X, topPos + 119, AllIcons.I_TARGET);
        layerBtn = new IconButton(BUTTONS_X, topPos + 139, ModIcons.I_LAYER_FRONT);

        acceptBtn.setToolTip(TOOLTIP_SAVE);
        cancelBtn.setToolTip(TOOLTIP_DISCARD);
        connectBtn.setToolTip(TOOLTIP_CONNECT);
        deleteBtn.setToolTip(TOOLTIP_DELETE);
        selectBtn.setToolTip(TOOLTIP_SELECT);
        layerBtn.setToolTip(TOOLTIP_LAYER);

        acceptBtn.withCallback(this::save);

        cancelBtn.withCallback(this::discard);

        connectBtn.withCallback(() -> toolSelect(Tool.CONNECT)); //toolCallback(this, Tool.CONNECT));
        deleteBtn.withCallback(() -> toolSelect(Tool.DELETE));//toolCallback(this, Tool.DELETE));
        selectBtn.withCallback(() -> toolSelect(Tool.SELECT));//toolCallback(this, Tool.SELECT));

        layerBtn.withCallback(() -> {
            backLayer = !backLayer;
            layerBtn.setIcon(backLayer ? ModIcons.I_LAYER_BACK : ModIcons.I_LAYER_FRONT);
        });

        editWidget.setSelectionCancelledCallback(() -> toolSelect(Tool.SELECT));
        toolSelect(Tool.SELECT);

        addRenderableWidget(nameField);
        addRenderableWidget(editWidget);
        addRenderableWidget(propertiesWidget);
        addRenderableWidget(acceptBtn);
        addRenderableWidget(cancelBtn);
        addRenderableWidget(connectBtn);
        addRenderableWidget(deleteBtn);
        addRenderableWidget(selectBtn);
        addRenderableWidget(layerBtn);
    }

    public Rect2i ghostTarget() {
        return new Rect2i(editWidget.getX(), editWidget.getY(), editWidget.getWidth(), editWidget.getHeight());
    }

    @Override
    protected List<net.minecraft.network.chat.Component> getTooltipFromContainerItem(ItemStack stack) {
        var lines = super.getTooltipFromContainerItem(stack);
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
        currentTool = tool;
        switch(tool) {
            case CONNECT -> editWidget.requestSelection(CircuitEditWidget.SelectMode.LINE, 0x80FFFFFF, this::placeTrace);
            case DELETE -> editWidget.requestSelection(CircuitEditWidget.SelectMode.AREA, 0x80FF8080, this::deleteArea);
            case SELECT -> editWidget.requestSelection(CircuitEditWidget.SelectMode.POINT, 0x80FFFFFF, this::selectComponent);
        }
    }

    private void toolSelect(Slot slot) {
        var component = Component.forItem(slot.getItem().getItem());
        if(component == null)
            return;
        editWidget.cancelSelection();
        if(selectedComponent != null) {
            selectedComponent = null;
            propertiesWidget.setComponent(null);
        }
        var placed = new PlacedComponent(component, 0, 0, UUID.randomUUID());
        editWidget.componentPlacement(placed, this::placeComponent);
        currentComponent = placed;
        selectedSlot = slot;
    }

    public void toolSelect(Item item) {
        var component = Component.forItem(item);
        if(component == null)
            return;
        editWidget.cancelSelection();
        if(selectedComponent != null) {
            selectedComponent = null;
            propertiesWidget.setComponent(null);
        }
        var placed = new PlacedComponent(component, 0, 0, UUID.randomUUID());
        editWidget.componentPlacement(placed, this::placeComponent);
        currentComponent = placed;
        selectedSlot = null;
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

        changed = true;
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
        playSound(ModdedSoundEvents.UI_DELETE_AREA);
        changed = true;
        return CircuitEditWidget.SelectionResult.BEGIN_NEW;
    }

    public CircuitEditWidget.SelectionResult selectComponent(int x1, int y1, int x2, int y2, int clickX, int clickY) {
        var placed = schematic.getComponent(x1 / GRID_TO_GRID_SCALE, y1 / GRID_TO_GRID_SCALE);
        if(placed == null) {
            playSound(ModdedSoundEvents.UI_FAIL);
            return CircuitEditWidget.SelectionResult.IGNORE;
        }

        selectedComponent = placed;
        propertiesWidget.setComponent(selectedComponent);
        playSound(ModdedSoundEvents.UI_SELECT_COMPONENT);
        changed = true;
        return CircuitEditWidget.SelectionResult.BEGIN_NEW;
    }

    public void placeComponent(int x, int y) {
        if(schematic.canPlace(currentComponent, x, y)) {
            playSound(ModdedSoundEvents.UI_PLACE_COMPONENT);
            schematic.placeComponent(currentComponent, x, y);
            changed = true;
        } else {
            playSound(ModdedSoundEvents.UI_FAIL);
        }
    }

    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
        int bgX = getLeftOfCentered(WIDTH);
        int invY = topPos + HEIGHT + 4;
        renderPlayerInventory(ctx, bgX + WIDTH - PLAYER_INVENTORY.getWidth(), invY);

        for(int k = 0; k < this.menu.slots.size(); ++k) {
            var slot = this.menu.slots.get(k);
            if(!slot.isActive() || !slot.hasItem())
                continue;
            if(slot == selectedSlot) {
                ctx.blit(BACKGROUND, leftPos + slot.x - 1, topPos + slot.y - 1 , 232, 18, 18, 18);
                continue;
            }
            if(Component.forItem(slot.getItem().getItem()) == null)
                continue;
            ctx.blit(BACKGROUND, leftPos + slot.x - 1, topPos + slot.y - 1 , 232, 0, 18, 18);
        }

        ctx.blit(BACKGROUND, bgX, topPos, 0, 0, WIDTH, HEIGHT);

        int bpX = bgX + 13, bpY = topPos + 22;
        if(!backLayer) {
            CircuitSchematicRender.renderLayer(bgLines, ctx, bpX, bpY, CIRCUIT_SCALE, COLOR_TRACE_BACK);
            CircuitSchematicRender.renderLayer(fgLines, ctx, bpX, bpY, CIRCUIT_SCALE, COLOR_TRACE_FRONT);
        } else {
            CircuitSchematicRender.renderLayer(fgLines, ctx, bpX, bpY, CIRCUIT_SCALE, COLOR_TRACE_BACK);
            CircuitSchematicRender.renderLayer(bgLines, ctx, bpX, bpY, CIRCUIT_SCALE, COLOR_TRACE_FRONT);
        }

        CircuitSchematicRender.renderComponents(schematic, ctx, bpX, bpY, CIRCUIT_SCALE, mouseX, mouseY);

        if(currentTool.y > 0) {
            ctx.blit(BACKGROUND, leftPos + 173 - 11, topPos + currentTool.y, 250, 0, 6, 18);
        }

        if(selectedComponent != null) {
            var footprint = selectedComponent.footprint();
            ctx.renderOutline(
                    bpX + selectedComponent.x * CIRCUIT_SCALE * GRID_TO_GRID_SCALE - 1, bpY + selectedComponent.y * CIRCUIT_SCALE * GRID_TO_GRID_SCALE - 1,
                    footprint.getWidth() * CIRCUIT_SCALE * GRID_TO_GRID_SCALE + 2, footprint.getHeight() * CIRCUIT_SCALE * GRID_TO_GRID_SCALE + 2,
                    COLOR_SELECT_OUTLINE
            );
        }
        
        if(unsavedPopupTimeout > 0) {
            int color = 0xFF6060;
            int alpha = Math.min(unsavedPopupTimeout, 20) * 255 / 20;
            color |= alpha << 24;
            ctx.drawCenteredString(font, TEXT_NOT_SAVED, width / 2, topPos - 12, color);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if(!this.menu.getCarried().isEmpty()) {
            toolSelect(this.menu.getCarried().getItem());
            this.menu.setCarried(ItemStack.EMPTY);
        }
        if(unsavedPopupTimeout > 0)
            --unsavedPopupTimeout;
    }

    @Override
    protected void renderTooltip(GuiGraphics context, int mouseX, int mouseY) {
        super.renderTooltip(context, mouseX, mouseY);

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
            context.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == 256) {
            if(!changed || saving) {
                this.onClose();
            } else {
                playSound(ModdedSoundEvents.UI_FAIL);
                unsavedPopupTimeout = 60;
            }
            return true;
        }
        // TODO: Add key binds for tools
        // Prevent closing on Inventory Key and ESC
        var focused = getFocused();
        var handled = focused != null && focused.keyPressed(keyCode, scanCode, modifiers);
        if(handled)
            return true;

        if(selectedComponent != null || focused instanceof EditBox)
            return false;

        // Hotbar as component quick select
        var hotbar = minecraft.options.keyHotbarSlots;
        for(int i = 0; i < hotbar.length; ++i) {
            if(hotbar[i].matches(keyCode, scanCode)) {
                toolSelect(menu.getSlot(i + 27));
                return true;
            }
        }

        // Other tool keybinds
        if(ModdedKeys.ROTATE_COMPONENT.matchesKey(keyCode, scanCode)) {
            if(currentComponent != null && currentComponent.has(Orientation.PROPERTY)) {
                var current = currentComponent.get(Orientation.PROPERTY);
                if(Screen.hasShiftDown()) {
                    current = current.getCounterClockwise();
                } else {
                    current = current.getClockwise();
                }
                currentComponent.set(Orientation.PROPERTY, current);
                playSound(ModdedSoundEvents.UI_COMPONENT_ROTATE);
                return true;
            }
        } else if(ModdedKeys.PLACE_TRACE.matchesKey(keyCode, scanCode)) {
            toolSelect(Tool.CONNECT);
            playSound(ModdedSoundEvents.UI_PLACE_TRACE);
            return true;
        } else if(ModdedKeys.DELETE_AREA.matchesKey(keyCode, scanCode)) {
            toolSelect(Tool.DELETE);
            playSound(ModdedSoundEvents.UI_DELETE_AREA);
            return true;
        } else if(ModdedKeys.PICK_COMPONENT.matchesKey(keyCode, scanCode)) {
            toolSelect(Tool.SELECT);
            playSound(ModdedSoundEvents.UI_SELECT_COMPONENT);
            return true;
        } else if(ModdedKeys.SWITCH_LAYER.matchesKey(keyCode, scanCode)) {
            backLayer = !backLayer;
            layerBtn.setIcon(backLayer ? ModIcons.I_LAYER_BACK : ModIcons.I_LAYER_FRONT);
            playSound(ModdedSoundEvents.UI_CLICK);
            return true;
        }

        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics ctx, int mouseX, int mouseY, float partialTicks) {
        super.render(ctx, mouseX, mouseY, partialTicks);

        if(saving) {
            RenderSystem.disableDepthTest();
            var ms = ctx.pose();
            ms.pushPose();
            ms.translate(0, 0, 10);
            // Apply another layer of gradient
            ctx.fillGradient(0, 0, width, height, -1072689136, -804253680);

            AllIcons.I_CONFIG_SAVE.render(ctx, width / 2 - 8, height / 2 - 8);
            ctx.drawCenteredString(font, TEXT_SAVING, width / 2, height / 2 + 12, -1);
            ms.popPose();
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType actionType) {
        if(slot == null || !slot.hasItem() || actionType != ClickType.PICKUP)
            return;
        toolSelect(slot);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(button == 1) {
            if(currentComponent == null && currentTool == Tool.SELECT) {
                int gridX = (int) ((mouseX - editWidget.getX()) / CIRCUIT_SCALE);
                int gridY = (int) ((mouseY - editWidget.getY()) / CIRCUIT_SCALE);
                if(gridX >= 0 && gridY >= 0 && gridX < GRID_SIZE && gridY < GRID_SIZE) {
                    schematic.removeComponents(gridX, gridY, 1, 1);
                }
            }
            if(currentComponent != null) {
                currentComponent = null;
                selectedSlot = null;
                editWidget.stopComponentPlacement();
            }
            if(selectedComponent != null) {
                selectedComponent = null;
                propertiesWidget.setComponent(null);
            }
            editWidget.cancelSelection();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private enum Tool {
        CONNECT(83),
        DELETE(101),
        SELECT(119);

        public final int y;

        Tool(int y) {
            this.y = y;
        }
    }
}
