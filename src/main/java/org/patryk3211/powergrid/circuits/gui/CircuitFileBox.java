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

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.utility.EditableScrollBox;
import org.patryk3211.powergrid.utility.Lang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

public class CircuitFileBox extends EditableScrollBox {
    private int tick;

    private static final Component TITLE = Lang.translateDirect("gui.circuit_designer.files");

    public CircuitFileBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message, TITLE);

        setTextColor(-1);
        setBordered(false);
        setEditable(true);
        refreshFiles();
        onChanged();
    }

    public void tick() {
        if(tick++ >= 20) {
            refreshFiles();
            tick = 0;
        }
    }

    private void refreshFiles() {
        try {
            Files.createDirectories(Paths.get("circuits"));
        } catch (IOException e) {
            PowerGrid.LOGGER.error("Failed to create a folder", e);
        }
        options.clear();

        try {
            Files.list(Paths.get("circuits/"))
                    .filter(f -> !Files.isDirectory(f) && f.getFileName().toString().endsWith(".nbt")).forEach(path -> {
                        if(Files.isDirectory(path))
                            return;

                        options.add(path.getFileName().toString());
                    });
        } catch (NoSuchFileException e) {
            // No Schematics created yet
        } catch (IOException e) {
            PowerGrid.LOGGER.error("Exception when loading circuit schematics", e);
        }

        // Copy of Create's schematic loader sort
        options.sort((aT, bT) -> {
            String a = aT;
            String b = bT;
            if (a.endsWith(".nbt"))
                a = a.substring(0, a.length() - 4);
            if (b.endsWith(".nbt"))
                b = b.substring(0, b.length() - 4);
            int aLength = a.length();
            int bLength = b.length();
            int minSize = Math.min(aLength, bLength);
            char aChar, bChar;
            boolean aNumber, bNumber;
            boolean asNumeric = false;
            int lastNumericCompare = 0;
            for (int i = 0; i < minSize; i++) {
                aChar = a.charAt(i);
                bChar = b.charAt(i);
                aNumber = aChar >= '0' && aChar <= '9';
                bNumber = bChar >= '0' && bChar <= '9';
                if (asNumeric)
                    if (aNumber && bNumber) {
                        if (lastNumericCompare == 0)
                            lastNumericCompare = aChar - bChar;
                    } else if (aNumber)
                        return 1;
                    else if (bNumber)
                        return -1;
                    else if (lastNumericCompare == 0) {
                        if (aChar != bChar)
                            return aChar - bChar;
                        asNumeric = false;
                    } else
                        return lastNumericCompare;
                else if (aNumber && bNumber) {
                    asNumeric = true;
                    if (lastNumericCompare == 0)
                        lastNumericCompare = aChar - bChar;
                } else if (aChar != bChar)
                    return aChar - bChar;
            }
            if (asNumeric)
                if (aLength > bLength && a.charAt(bLength) >= '0' && a.charAt(bLength) <= '9') // as number
                    return 1; // a has bigger size, thus b is smaller
                else if (bLength > aLength && b.charAt(aLength) >= '0' && b.charAt(aLength) <= '9') // as number
                    return -1; // b has bigger size, thus a is smaller
                else if (lastNumericCompare == 0)
                    return aLength - bLength;
                else
                    return lastNumericCompare;
            else
                return aLength - bLength;
        });

        int prev = selectedIndex;
        clamp();
        if(prev != selectedIndex)
            onChanged();
        updateTooltip();
    }
}
