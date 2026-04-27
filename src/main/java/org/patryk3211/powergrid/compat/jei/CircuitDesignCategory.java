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
package org.patryk3211.powergrid.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.compat.jei.ItemIcon;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.utility.Lang;

public class CircuitDesignCategory implements IRecipeCategory<Object> {
    public static final ResourceLocation IDENTIFIER = PowerGrid.asResource("circuit_design");
    public static final RecipeType<Object> TYPE = new RecipeType<>(IDENTIFIER, Object.class);
    public static final ResourceLocation TEXTURE = PowerGrid.texture("gui/circuit_design_rei_overlay");

    private final IDrawable icon;

    public CircuitDesignCategory() {
        icon = new ItemIcon(ModdedBlocks.CIRCUIT_DESIGN_TABLE::asStack);
    }

    @Override
    public RecipeType<Object> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Lang.translateDirect("recipe.category.circuit_design");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 100;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Object recipe, IFocusGroup focuses) {
        var schematicIn = builder.addInputSlot(21, 38)
//                .setOutputSlotBackground()
                .addItemLike(AllItems.EMPTY_SCHEMATIC);
        var schematicOut = builder.addOutputSlot(113, 38)
                .addItemLike(ModdedItems.CIRCUIT_SCHEMATIC);
        var components = builder.addInputSlot(67, 15)
                .addIngredients(Ingredient.of(ModdedTags.Item.CIRCUIT_COMPONENT.tag));
    }

    @Override
    public void draw(Object recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.blit(TEXTURE, 0, 0, 0, 0, getWidth(), getHeight());

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(75 - 24 - 3, 4 + 65 - 24 - 3, 0);
        poseStack.scale(3, 3, 1);
        icon.draw(guiGraphics);
        poseStack.popPose();
    }
}
