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

import com.simibubi.create.Create;
import com.simibubi.create.compat.rei.category.CreateRecipeCategory;
import com.simibubi.create.compat.rei.category.WidgetUtil;
import com.simibubi.create.compat.rei.display.CreateDisplay;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.patryk3211.powergrid.electricity.electromagnet.recipe.MagnetizingRecipe;
import org.patryk3211.powergrid.utility.Lang;

import java.util.ArrayList;
import java.util.List;

public class MagnetizingCategory extends CreateRecipeCategory<MagnetizingRecipe> {
    private final AnimatedMagnet magnet = new AnimatedMagnet();

    public MagnetizingCategory(Info<MagnetizingRecipe> info) {
        super(info);
    }

    @Override
    public List<Widget> setupDisplay(CreateDisplay<MagnetizingRecipe> display, Rectangle bounds) {
        Point origin = new Point(bounds.getX(), bounds.getY() + 15);
        List<Widget> widgets = new ArrayList<>();
        List<ProcessingOutput> results = display.getRecipe().getRollableResults();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createSlot(new Point(origin.x + 27, origin.y + 51)).disableBackground().markInput().entries(display.getInputEntries().get(0)));
        widgets.add(WidgetUtil.textured(AllGuiTextures.JEI_SLOT, origin.x + 26, origin.y + 50));
        widgets.add(WidgetUtil.textured(AllGuiTextures.JEI_SHADOW, origin.x + 61, origin.y + 41));
        widgets.add(WidgetUtil.textured(AllGuiTextures.JEI_LONG_ARROW, origin.x + 52, origin.y + 54));

        for (int outputIndex = 0; outputIndex < results.size(); outputIndex++) {
            List<Component> tooltip = new ArrayList<>();
            if (results.get(outputIndex).getChance() != 1) {
                Lang.builder(Create.ID)
                        .translate("recipe.processing.chance", results.get(outputIndex).getChance() < 0.01 ? "<1" : (int) (results.get(outputIndex).getChance() * 100))
                        .style(ChatFormatting.GOLD)
                        .addTo(tooltip);
            }
            widgets.add(Widgets.createSlot(new Point((origin.x + 131 + 19 * outputIndex) + 1, (origin.y + 50) + 1))
                    .disableBackground().markOutput()
                    .entry(EntryStack.of(VanillaEntryTypes.ITEM, results.get(outputIndex).getStack()).tooltip(tooltip)));
            widgets.add(WidgetUtil.textured(getRenderedSlot(display.getRecipe(), outputIndex), origin.x + 131 + 19 * outputIndex, origin.y + 50));
        }
        magnet.setPos(new Point(origin.getX() + (getDisplayWidth(display) / 2 - 17), origin.getY() + 22));
        widgets.add(magnet);
        return widgets;
    }
}
