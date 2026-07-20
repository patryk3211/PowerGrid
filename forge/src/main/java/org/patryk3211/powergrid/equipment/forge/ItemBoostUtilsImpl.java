package org.patryk3211.powergrid.equipment.forge;

import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class ItemBoostUtilsImpl {
    public static RecipeHolder<?> findRecipe(Level level, ItemStack chip, ItemStack toBoost) {
        var inv = new ItemStackHandler(2);
        var wrap = new RecipeWrapper(inv);
        inv.setStackInSlot(0, toBoost);
        inv.setStackInSlot(1, chip);
        return AllRecipeTypes.DEPLOYING.find(wrap, level).orElse(null);
    }
}
