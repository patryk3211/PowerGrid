package org.patryk3211.powergrid.equipment;

import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class BoostRecipe extends DeployerApplicationRecipe {
    public static final RecipeSerializer<BoostRecipe> SERIALIZER = new ProcessingRecipeSerializer<>(BoostRecipe::new);

    public BoostRecipe(ProcessingRecipeBuilder.ProcessingRecipeParams params) {
        super(params);
    }

    @Override
    public boolean matches(Container inv, Level level) {
        return super.matches(inv, level) && !ItemBoostUtils.isBoosted(inv.getItem(0));
    }

    @Override
    public List<ItemStack> rollResults(List<ProcessingOutput> rollableResults) {
        List<ItemStack> results = new ArrayList<>();
        ProcessingOutput output = rollableResults.get(0);
        ItemStack stack = output.rollOutput();
        if (!stack.isEmpty()) {
            ItemBoostUtils.setBoosted(stack, true);
            results.add(stack);
        }
        return results;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
