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

import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import java.util.function.BiConsumer;

import static org.patryk3211.powergrid.circuits.editor.CircuitDesignTableEditScreen.CIRCUIT_SCALE;
import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_SIZE;
import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_TO_GRID_SCALE;

public class CircuitEditWidget extends AbstractSimiWidget {
    private int scale = CIRCUIT_SCALE;

    private final Font textRenderer;
    private boolean selectStarted = false;
    private int startX, startY;

    private SelectMode selectMode = SelectMode.NONE;
    private int selectionColor = 0;
    private SelectCallback selectionCallback = null;
    private Runnable selectionCancelledCallback = null;

    private PlacedComponent placedComponent = null;
    private BiConsumer<Integer, Integer> placementCallback;

    private final CircuitSchematic schematic;

    public CircuitEditWidget(Font textRenderer, CircuitSchematic schematic, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.textRenderer = textRenderer;
        this.schematic = schematic;
    }

    @Override
    protected void doRender(@NotNull GuiGraphics ctx, int mouseX, int mouseY, float partialTicks) {
        if (!isHovered)
            return;
        int x = getX();
        int y = getY();

        int gridX = (mouseX - x) / scale;
        int gridY = (mouseY - y) / scale;
        if(gridX >= GRID_SIZE || gridY >= GRID_SIZE)
            return;

        var ms = ctx.pose();
        ms.translate(x, y, 5);
        ms.scale(scale, scale, scale);

        if(placedComponent != null) {
            var footprint = placedComponent.footprint();
            gridX /= GRID_TO_GRID_SCALE;
            gridY /= GRID_TO_GRID_SCALE;
            int offsetX = footprint.getWidth() / 2;
            int offsetY = footprint.getHeight() / 2;
            footprint.render(ctx, (gridX - offsetX) * GRID_TO_GRID_SCALE, (gridY - offsetY) * GRID_TO_GRID_SCALE);
            footprint.renderPadIndices(ctx, textRenderer, (gridX - offsetX) * GRID_TO_GRID_SCALE, (gridY - offsetY) * GRID_TO_GRID_SCALE);

            ms.pushPose();
            ms.scale(1f / scale, 1f / scale, 1f / scale);
            int color = schematic.canPlace(placedComponent, gridX - offsetX, gridY - offsetY) ? 0x8080FF80 : 0x80FF8080;
            ctx.renderOutline((gridX - offsetX) * scale * GRID_TO_GRID_SCALE, (gridY - offsetY) * scale * GRID_TO_GRID_SCALE, footprint.getWidth() * scale * GRID_TO_GRID_SCALE, footprint.getHeight() * scale * GRID_TO_GRID_SCALE, color);
            ms.popPose();
            return;
        }

        if(selectMode == SelectMode.NONE) {
            // Draw cursor
            ms.pushPose();
            ms.scale(1f / scale, 1f / scale, 1f / scale);
            ctx.renderOutline(gridX * scale, gridY * scale, scale, scale, 0xFFAAAAFF);
            ms.popPose();
        } else if(selectMode == SelectMode.POINT) {
            ms.pushPose();
            ms.translate(gridX, gridY, 0);
            ms.scale(0.25f, 0.25f, 0.25f);
            ctx.fill(1, 1, 3, 3, selectionColor);
            ctx.fill(-6, 1, -1, 3, selectionColor);
            ctx.fill(1, -6, 3, -1, selectionColor);
            ctx.fill(5, 1, 10, 3, selectionColor);
            ctx.fill(1, 5, 3, 10, selectionColor);
            ms.popPose();
        } else if(!selectStarted) {
            ctx.fill(gridX, gridY, gridX + 1, gridY + 1, selectionColor);
        } else if(selectMode == SelectMode.LINE) {
            int lenX = Math.abs(gridX - startX) + 1;
            int lenY = Math.abs(gridY - startY) + 1;
            if(lenX >= lenY) {
                // Horizontal
                var x1 = Math.min(gridX, startX);
                var x2 = x1 + lenX;
                ctx.fill(x1, startY, x2, startY + 1, selectionColor);
            } else {
                // Vertical
                var y1 = Math.min(gridY, startY);
                var y2 = y1 + lenY;
                ctx.fill(startX, y1, startX + 1, y2, selectionColor);
            }
        } else if(selectMode == SelectMode.AREA) {
            var x1 = Math.min(startX, gridX);
            var y1 = Math.min(startY, gridY);
            var x2 = Math.max(startX, gridX) + 1;
            var y2 = Math.max(startY, gridY) + 1;
            ctx.fill(x1, y1, x2, y2, selectionColor);
        }
    }

    private void handleCallback(int endX, int endY) {
        SelectionResult result = SelectionResult.END;
        if(selectionCallback != null) {
            int x1, y1, x2, y2;
            switch(selectMode) {
                case POINT -> {
                    x1 = x2 = endX;
                    y1 = y2 = endY;
                }
                case LINE -> {
                    int lenX = Math.abs(endX - startX);
                    int lenY = Math.abs(endY - startY);
                    if(lenX >= lenY) {
                        // Horizontal
                        x1 = Math.min(endX, startX);
                        x2 = x1 + lenX;
                        y1 = y2 = startY;
                        endY = startY;
                    } else {
                        // Vertical
                        y1 = Math.min(endY, startY);
                        y2 = y1 + lenY;
                        x1 = x2 = startX;
                        endX = startX;
                    }
                }
                case AREA -> {
                    x1 = Math.min(startX, endX);
                    y1 = Math.min(startY, endY);
                    x2 = Math.max(startX, endX);
                    y2 = Math.max(startY, endY);
                }
                default -> throw new IllegalStateException("Cannot handle callback without valid selection mode");
            }
            result = selectionCallback.accept(x1, y1, x2, y2, endX, endY);
        }
        switch(result) {
            case CONTINUE -> {
                startX = endX;
                startY = endY;
                selectStarted = true;
            }
            case END -> {
                selectStarted = false;
                selectMode = SelectMode.NONE;
                selectionCallback = null;
            }
            case BEGIN_NEW -> {
                selectStarted = false;
            }
            case IGNORE -> { }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int gridX = (int) ((mouseX - getX()) / scale);
        int gridY = (int) ((mouseY - getY()) / scale);
        if(gridX < 0 || gridX >= GRID_SIZE || gridY < 0 || gridY >= GRID_SIZE)
            return false;

        if(placedComponent != null) {
            gridX /= GRID_TO_GRID_SCALE;
            gridY /= GRID_TO_GRID_SCALE;
            var footprint = placedComponent.footprint();
            int offsetX = footprint.getWidth() / 2;
            int offsetY = footprint.getHeight() / 2;
            if(button == 0) {
                placementCallback.accept(gridX - offsetX, gridY - offsetY);
            }
            return true; //super.mouseClicked(mouseX, mouseY, button);
        }

        if(button == 0 && selectMode != SelectMode.NONE) {
            if(selectMode == SelectMode.POINT) {
                startX = gridX;
                startY = gridY;
                handleCallback(gridX, gridY);
            } else {
                if(!selectStarted) {
                    startX = gridX;
                    startY = gridY;
                    selectStarted = true;
                    playDownSound(Minecraft.getInstance().getSoundManager());
                } else {
                    handleCallback(gridX, gridY);
                }
            }
        } else if (button == 1) {
            cancelSelection();
        }
        return true; //super.mouseClicked(mouseX, mouseY, button);
    }

    public void requestSelection(SelectMode mode, int color, SelectCallback callback) {
        selectStarted = false;
        selectMode = mode;
        selectionColor = color;
        selectionCallback = callback;
    }

    public void setSelectionCancelledCallback(Runnable callback) {
        selectionCancelledCallback = callback;
    }

    public void cancelSelection() {
        if(selectMode != SelectMode.NONE) {
            if(selectionCancelledCallback != null)
                selectionCancelledCallback.run();
            selectMode = SelectMode.NONE;
            selectionCallback = null;
            selectStarted = false;
        }
    }

    public void componentPlacement(PlacedComponent component, BiConsumer<Integer, Integer> callback) {
        placedComponent = component;
        placementCallback = callback;
    }

    public void stopComponentPlacement() {
        placedComponent = null;
        placementCallback = null;
    }

    public enum SelectMode {
        NONE, POINT, LINE, AREA
    }

    public enum SelectionResult {
        IGNORE, CONTINUE, BEGIN_NEW, END
    }

    public interface SelectCallback {
        SelectionResult accept(int x1, int y1, int x2, int y2, int clickX, int clickY);
    }
}
