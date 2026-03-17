/*
 * Copyright 2026 patryk3211
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
package org.patryk3211.powergrid.electricity.info.customdisplay;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModIcons;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.SetCustomDisplayC2SPacket;
import org.patryk3211.powergrid.utility.EditableScrollBox;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.Arrays;
import java.util.List;

public class CustomDisplayScreen extends AbstractSimiContainerScreen<CustomDisplayMenu> {
    protected static final ResourceLocation TEXTURE = PowerGrid.texture("gui/custom_display");
    private static final int WIDTH = 180, HEIGHT = 107;

    private static final Component TOOLTIP_EXPR = Lang.translateDirect("gui.custom_display.expr");
    private static final Component TOOLTIP_UNIT = Lang.translateDirect("gui.custom_display.unit");
    private static final Component TOOLTIP_PREFIX = Lang.translateDirect("gui.custom_display.prefix");

    private final ItemStack stack;

    private EditBox equationField;
    private EditableScrollBox unitSelector;

    public CustomDisplayScreen(CustomDisplayMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        stack = new ItemStack(container.contentHolder.getBlockState().getBlock());
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT);

        super.init();

        var prefixBtn = new IconButton(leftPos + 139, topPos + 48, ModIcons.I_PREFIXES);
        prefixBtn.setToolTip(TOOLTIP_PREFIX);
        prefixBtn.withCallback(() -> menu.enablePrefixes = !menu.enablePrefixes);

        var saveBtn = new IconButton(leftPos + 147, topPos + 83, AllIcons.I_CONFIRM);
        saveBtn.withCallback(this::onClose);

        equationField = new EditBox(font, leftPos + 42, topPos + 31, 114, 9, CommonComponents.EMPTY);
        equationField.setValue(menu.expression);
        equationField.setTextColor(-1);
        equationField.setBordered(false);
        equationField.setMaxLength(100);
        equationField.setEditable(true);
        equationField.setResponder(str -> menu.expression = str);

        unitSelector = new EditableScrollBox(font, leftPos + 42, topPos + 53, 90, 9, CommonComponents.EMPTY, TOOLTIP_UNIT);
        unitSelector.setOptions(Arrays.stream(Unit.values()).map(Unit::string).toList());
        if(menu.unit != null) {
            unitSelector.setState(menu.unit.ordinal());
        } else {
            unitSelector.setValue(menu.unitStr);
        }
        unitSelector.calling(i -> {
            menu.unit = Unit.values()[i];
            menu.unitStr = null;
        }, str -> {
            menu.unit = null;
            menu.unitStr = str;
        });

        addRenderableWidget(prefixBtn);
        addRenderableWidget(saveBtn);
        addRenderableWidget(new TooltipWidget(leftPos + 15, topPos + 26, 18, 18, TOOLTIP_EXPR));
        addRenderableWidget(new TooltipWidget(leftPos + 15, topPos + 48, 18, 18, TOOLTIP_UNIT));
        addRenderableWidget(unitSelector);
        addRenderableWidget(equationField);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        unitSelector.tick();
        equationField.tick();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        var result = super.keyPressed(pKeyCode, pScanCode, pModifiers);
        if(getFocused() == equationField)
            return true;
        if(getFocused() == unitSelector)
            return true;
        return result;
    }

    @Override
    protected void renderForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderForeground(graphics, mouseX, mouseY, partialTicks);
        if(unitSelector != null && unitSelector.isMouseOver(mouseX, mouseY)) {
            List<Component> tooltip = unitSelector.getToolTip();
            if(tooltip.isEmpty())
                return;
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics context, float partialTick, int mouseX, int mouseY) {
        int bgX = getLeftOfCentered(WIDTH);
        context.blit(TEXTURE, bgX, topPos, 0, 0, WIDTH, HEIGHT);

        context.drawCenteredString(font, Lang.translateDirect("gui.custom_display.title"), bgX + WIDTH / 2, topPos + 3, -1);

        if(menu.enablePrefixes) {
            AllGuiTextures.INDICATOR_GREEN.render(context, bgX + 139, topPos + 66);
        }

        var pose = context.pose();
        pose.pushPose();
        pose.scale(2, 2, 1);
        context.renderItem(stack, (bgX + 180) / 2, (topPos + 77) / 2);
        pose.popPose();
    }

    @Override
    public void onClose() {
        super.onClose();
        ModdedPackets.sendToServer(new SetCustomDisplayC2SPacket(menu.contentHolder, menu.expression, menu.enablePrefixes, menu.unit, menu.unitStr));
    }
}
