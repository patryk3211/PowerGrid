package org.patryk3211.powergrid.equipment.forge;

import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public class ItemBoostUtilsImpl {
    public static Recipe<?> findRecipe(Level level, ItemStack chip, ItemStack toBoost) {
        var inv = new RecipeWrapper(new ItemStackHandler(2));
        inv.setItem(0, toBoost);
        inv.setItem(1, chip);
        var recipes = level.getRecipeManager().getRecipeFor(AllRecipeTypes.DEPLOYING.getType(), inv, level);
        return recipes.orElse(null);
    }
}
