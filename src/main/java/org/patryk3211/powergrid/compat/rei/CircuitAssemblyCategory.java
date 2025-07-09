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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.rei.ItemIcon;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.utility.Lang;

import java.util.ArrayList;
import java.util.List;

public class CircuitAssemblyCategory implements DisplayCategory<CircuitAssemblyDisplay> {
    public static final CategoryIdentifier<CircuitAssemblyDisplay> IDENTIFIER = CategoryIdentifier.of(PowerGrid.asResource("circuit_assembly"));
    public static final Identifier TEXTURE = PowerGrid.texture("gui/circuit_assembly_rei_overlay");
    private final AnimatedMechanicalArm arm = new AnimatedMechanicalArm();

    private final Renderer icon;

    public CircuitAssemblyCategory() {
        icon = new ItemIcon(AllBlocks.MECHANICAL_ARM::asStack);
    }

    @Override
    public CategoryIdentifier<? extends CircuitAssemblyDisplay> getCategoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Text getTitle() {
        return Lang.translateDirect("recipe.category.circuit_assembly");
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public int getDisplayHeight() {
        return 90;
    }

    @Override
    public int getDisplayWidth(CircuitAssemblyDisplay display) {
        return 150;
    }

    private void renderScene(DrawContext graphics, int mouseX, int mouseY, float partialTick, Rectangle bounds) {
        MatrixStack poseStack = graphics.getMatrices();
        poseStack.push();
        poseStack.translate(bounds.getX() + 60, bounds.getY() + 4 + 30, 0);

        arm.draw(graphics, -14, -16);

        poseStack.push();
        var depot = new ItemIcon(AllBlocks.DEPOT::asStack);
        poseStack.translate(35 - 8,  -8, -10);
        poseStack.scale(2, 2, 1);
        depot.render(graphics, new Rectangle(0, 0, 16, 16), mouseX, mouseY, partialTick);
        poseStack.pop();

        poseStack.pop();
    }

    @Override
    public List<Widget> setupDisplay(CircuitAssemblyDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, partialTick) ->
                renderScene(graphics, mouseX, mouseY, partialTick, bounds)));
        widgets.add(Widgets.createTexturedWidget(TEXTURE, bounds, 0, 0));

        var componentSlot = Widgets.createSlot(new Point(bounds.getX() + 12, bounds.getY() + 4 + 32));
        componentSlot.entries(display.getComponents());
        componentSlot.markInput();
        widgets.add(componentSlot);

        arm.heldItemSupplier = () -> {
            var entry = componentSlot.getCurrentEntry();
            if(entry.getType() == VanillaEntryTypes.ITEM) {
                return (ItemStack) entry.getValue();
            }
            return ItemStack.EMPTY;
        };

        var depotSlot = Widgets.createSlot(new Point(bounds.getX() + 95, bounds.getY() + 4 + 30 - 8));
        depotSlot.entry(EntryStack.of(VanillaEntryTypes.ITEM, ModdedItems.INCOMPLETE_CIRCUIT.asStack()));
        depotSlot.markInput();
        depotSlot.disableBackground();
        widgets.add(depotSlot);

        var resultSlot = Widgets.createSlot(new Point(bounds.getX() + 95, bounds.getY() + 4 + 60));
        resultSlot.entry(EntryStack.of(VanillaEntryTypes.ITEM, ModdedBlocks.CIRCUIT_BOARD.asStack()));
        resultSlot.markOutput();
        resultSlot.disableBackground();
        widgets.add(resultSlot);

        return widgets;
    }
}
