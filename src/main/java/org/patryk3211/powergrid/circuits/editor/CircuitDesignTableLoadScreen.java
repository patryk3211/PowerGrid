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
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.gui.CircuitFileBox;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.collections.ModIcons;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.network.packets.ChangeScreenC2SPacket;
import org.patryk3211.powergrid.network.packets.SaveSchematicC2SPacket;
import org.patryk3211.powergrid.utility.Lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CircuitDesignTableLoadScreen extends AbstractSimiContainerScreen<CircuitDesignTableLoadMenu> {
    private static final ResourceLocation BACKGROUND = PowerGrid.texture("gui/circuit_design_table_load");
    private static final int WIDTH = 214;
    private static final int HEIGHT = 85;

    private static final Component NO_NAME = Lang.translateDirect("gui.circuit_designer.no_name");
    private static final Component CONFIRM_OVERWRITE = Lang.translateDirect("gui.circuit_designer.confirm_overwrite");
    private static final Component DOESNT_EXIST = Lang.translateDirect("gui.circuit_designer.doesnt_exist");
    private static final Component INVALID_FILE = Lang.translateDirect("gui.circuit_designer.invalid_file");

    private static final Component FILE_DIALOG = Lang.translateDirect("gui.circuit_designer.file_dialog");
    private static final Component CANCEL = Lang.translateDirect("gui.circuit_designer.cancel");
    private static final Component FILE_SAVE = Lang.translateDirect("gui.circuit_designer.file_save");
    private static final Component FILE_LOAD = Lang.translateDirect("gui.circuit_designer.file_load");

    private IconButton cancelBtn;
    private IconButton saveBtn;
    private IconButton loadBtn;
    private IconButton folderBtn;

    private CircuitFileBox fileNameInput;

    private Component popupText;
    private int popupTimeout;
    private boolean confirm;

    public CircuitDesignTableLoadScreen(CircuitDesignTableLoadMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    private static SoundManager soundManager() {
        return Minecraft.getInstance().getSoundManager();
    }

    private static void playSound(AllSoundEvents.SoundEntry sound) {
        soundManager().play(SimpleSoundInstance.forUI(sound.getMainEvent(), 1.0f));
    }

    private void save() {
        if(fileNameInput.getValue().isEmpty()) {
            popup(NO_NAME);
            return;
        }

        try {
            var file = Path.of("circuits", fileNameInput.getValue());
            if(!file.toString().endsWith(".nbt"))
                file = Path.of(file + ".nbt");
            Files.createDirectories(file.getParent());
            if(Files.exists(file) && !confirm) {
                confirm = true;
                popup(CONFIRM_OVERWRITE);
                return;
            }
            Files.deleteIfExists(file);
            try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE)) {
                NbtIo.writeCompressed(this.menu.contentHolder.schematic.serializeNbt(), out);
            }
            back();
        } catch (IOException e) {
            PowerGrid.LOGGER.error("Failed to save circuit schematic", e);
        }
        confirm = false;
    }

    private void load() {
        var file = Path.of("circuits", fileNameInput.getValue());
        if(!Files.exists(file)) {
            popup(DOESNT_EXIST);
            return;
        }
        try {
            try (InputStream in = Files.newInputStream(file, StandardOpenOption.READ)) {
                var nbt = NbtIo.readCompressed(in);
                var schematic = CircuitSchematic.fromNbt(nbt);
                ModdedPackets.getChannel().sendToServer(new SaveSchematicC2SPacket(this.menu.contentHolder, null, schematic));
            }
        } catch (IOException e) {
            PowerGrid.LOGGER.error("Failed to load circuit schematic", e);
        } catch (RuntimeException e) {
            popup(INVALID_FILE);
        }
    }

    private void back() {
        ModdedPackets.getChannel().sendToServer(new ChangeScreenC2SPacket(menu.contentHolder, 0));
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT);
        super.init();

        cancelBtn = new IconButton(leftPos + 20, topPos + 58, ModIcons.I_CANCEL);
        cancelBtn.withCallback(this::back);
        cancelBtn.setToolTip(CANCEL);
        saveBtn = new IconButton(leftPos + 146, topPos + 58, AllIcons.I_CONFIG_SAVE);
        saveBtn.withCallback(this::save);
        saveBtn.setToolTip(FILE_SAVE);
        loadBtn = new IconButton(leftPos + 168, topPos + 58, ModIcons.I_UPLOAD);
        loadBtn.withCallback(this::load);
        loadBtn.setToolTip(FILE_LOAD);

        folderBtn = new IconButton(leftPos + 20, topPos + 23, AllIcons.I_OPEN_FOLDER);
        folderBtn.withCallback(() -> Util.getPlatform().openFile(Paths.get("circuits/").toFile()));

        fileNameInput = new CircuitFileBox(font, leftPos + 48, topPos + 28, 135, 10, Component.empty());

        addRenderableWidget(cancelBtn);
        addRenderableWidget(saveBtn);
        addRenderableWidget(loadBtn);
        addRenderableWidget(folderBtn);
        addRenderableWidget(fileNameInput);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if(popupTimeout > 0)
            --popupTimeout;
    }

    private void popup(Component text) {
        playSound(ModdedSoundEvents.UI_FAIL);
        this.popupText = text;
        this.popupTimeout = 60;
    }

    @Override
    protected void renderBg(GuiGraphics ctx, float partialTick, int mouseX, int mouseY) {
        int bgX = getLeftOfCentered(WIDTH);
//        int invY = topPos + HEIGHT + 4;
//        renderPlayerInventory(ctx, bgX + 2, invY);

        ctx.blit(BACKGROUND, bgX, topPos, 0, 0, WIDTH, HEIGHT);
        ctx.drawString(font, FILE_DIALOG, leftPos + 4, topPos + 3, 0x404040, false);

        if(popupTimeout > 0) {
            int color = 0xFF6060;
            int alpha = Math.min(popupTimeout, 20) * 255 / 20;
            color |= alpha << 24;
            ctx.drawCenteredString(font, popupText, width / 2, topPos - 12, color);
        }
    }
}
