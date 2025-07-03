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
package org.patryk3211.powergrid.compat.rei;

import com.simibubi.create.AllItems;
import com.simibubi.create.compat.rei.ItemIcon;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.utility.Lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CircuitDesignCategory implements DisplayCategory<CircuitDesignDisplay> {
    public static final CategoryIdentifier<CircuitDesignDisplay> IDENTIFIER = CategoryIdentifier.of(PowerGrid.asResource("circuit_design"));
    public static final Identifier TEXTURE = PowerGrid.texture("gui/circuit_design_rei_overlay");

    private final Renderer icon;

    public CircuitDesignCategory() {
        icon = new ItemIcon(ModdedBlocks.CIRCUIT_DESIGN_TABLE::asStack);
    }

    @Override
    public CategoryIdentifier<? extends CircuitDesignDisplay> getCategoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Text getTitle() {
        return Lang.translateDirect("recipe.category.circuit_design");
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public int getDisplayWidth(CircuitDesignDisplay display) {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 150;
    }

    @Override
    public List<Widget> setupDisplay(CircuitDesignDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, partialTick) -> {
            MatrixStack poseStack = graphics.getMatrices();
            poseStack.push();
            poseStack.translate(bounds.getX() + 75 - 24, bounds.getY() + 4 + 75 - 24, 0);
            poseStack.scale(3, 3, 1);
            icon.render(graphics, new Rectangle(0, 0, 16, 16), mouseX, mouseY, partialTick);
            poseStack.pop();
        }));
        widgets.add(Widgets.createTexturedWidget(TEXTURE, bounds, 0, 0));

        var schematicIn = Widgets.createSlot(new Point(bounds.x + 21, bounds.y + 89));
        schematicIn.entry(EntryStack.of(VanillaEntryTypes.ITEM, AllItems.EMPTY_SCHEMATIC.asStack()));
        schematicIn.markInput();
        widgets.add(schematicIn);

        var schematicOut = Widgets.createSlot(new Point(bounds.x + 113, bounds.y + 89));
        schematicOut.entry(EntryStack.of(VanillaEntryTypes.ITEM, ModdedItems.CIRCUIT_SCHEMATIC.asStack()));
        schematicOut.markOutput();
        widgets.add(schematicOut);

        var components = Widgets.createSlot(new Point(bounds.x + 67, bounds.y + 25));
        components.entries(display.getInputEntries()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
        components.markInput();
        widgets.add(components);
        return widgets;
    }
}
