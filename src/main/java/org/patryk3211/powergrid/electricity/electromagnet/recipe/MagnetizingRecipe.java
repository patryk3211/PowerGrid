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
package org.patryk3211.powergrid.electricity.electromagnet.recipe;

import com.simibubi.create.compat.recipeViewerCommon.SequencedAssemblySubCategoryType;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;
import java.util.Set;

public class MagnetizingRecipe extends ProcessingRecipe<Inventory> implements IAssemblyRecipe {
    public static final TypeInfo TYPE_INFO = new TypeInfo();

    public MagnetizingRecipe(ProcessingRecipeBuilder.ProcessingRecipeParams params) {
        super(TYPE_INFO, params);
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 3;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Text getDescriptionForAssembly() {
        return Lang.translateDirect("recipe.assembly.magnetizing");
    }

    @Override
    public void addRequiredMachines(Set<ItemConvertible> list) {
        list.add(ModdedBlocks.ELECTROMAGNET.get());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {

    }

    @Override
    public SequencedAssemblySubCategoryType getJEISubCategory() {
        return null;
    }

    @Override
    public boolean matches(Inventory inventory, World world) {
        if(inventory.isEmpty())
            return false;
        return ingredients.get(0).test(inventory.getStack(0));
    }
}
