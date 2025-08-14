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
import com.simibubi.create.AllBlocks;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.utility.Lang;

public class CircuitAssemblyCategory implements IRecipeCategory<Object> {
    public static final ResourceLocation IDENTIFIER = PowerGrid.asResource("circuit_assembly");
    public static final RecipeType<Object> TYPE = new RecipeType<>(IDENTIFIER, Object.class);

    public static final ResourceLocation TEXTURE = PowerGrid.texture("gui/circuit_assembly_rei_overlay");
    private final AnimatedMechanicalArm arm = new AnimatedMechanicalArm();

    private final IDrawable icon;

    public CircuitAssemblyCategory() {
        icon = new ItemIcon(AllBlocks.MECHANICAL_ARM::asStack);
    }

    @NotNull
    @Override
    public RecipeType<Object> getRecipeType() {
        return TYPE;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Lang.translateDirect("recipe.category.circuit_assembly");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Object recipe, IFocusGroup focuses) {
        var componentSlot = builder.addInputSlot(12, 4 + 32)
                .setStandardSlotBackground()
                .addIngredients(Ingredient.of(ModdedTags.Item.CIRCUIT_COMPONENT.tag));
        var depotSlot = builder.addInputSlot(95, 4 + 30 - 8)
                .addItemLike(ModdedItems.INCOMPLETE_CIRCUIT);
        var resultSlot = builder.addOutputSlot(95, 4 + 60)
                .addItemLike(ModdedBlocks.CIRCUIT_BOARD);
    }

    @Override
    public int getHeight() {
        return 90;
    }

    @Override
    public int getWidth() {
        return 150;
    }

    private void renderScene(GuiGraphics graphics) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(60, 4 + 30, 0);

        arm.draw(graphics, -14, -16);

        poseStack.pushPose();
        var depot = new ItemIcon(AllBlocks.DEPOT::asStack);
        poseStack.translate(35 - 8 - 2,  -8 - 2, -10);
        poseStack.scale(2, 2, 1);
        depot.draw(graphics, 0, 0);
//        depot.render(graphics, new Rectangle(0, 0, 16, 16), mouseX, mouseY, partialTick);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public void draw(Object recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
        guiGraphics.blit(TEXTURE, 0, 0, 0, 0, getWidth(), getHeight());
        renderScene(guiGraphics);
//        List<Widget> widgets = new ArrayList<>();
//        widgets.add(Widgets.createRecipeBase(bounds));
//        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, partialTick) ->
//                renderScene(graphics, mouseX, mouseY, partialTick, bounds)));
//        widgets.add(Widgets.createTexturedWidget(TEXTURE, bounds, 0, 0));
//        guiGraphics.

        arm.heldItemSupplier = () ->
            recipeSlotsView.getSlotViews().get(0).getDisplayedItemStack().orElse(ItemStack.EMPTY);
    }
}
