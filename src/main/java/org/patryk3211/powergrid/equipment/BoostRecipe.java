package org.patryk3211.powergrid.equipment;

import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipeParams;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BoostRecipe extends DeployerApplicationRecipe {
    public static final RecipeSerializer<BoostRecipe> SERIALIZER = new ItemApplicationRecipe.Serializer<>(BoostRecipe::new);

    public BoostRecipe(ItemApplicationRecipeParams params) {
        super(params);
    }

    @Override
    public boolean matches(RecipeWrapper inv, Level level) {
        return super.matches(inv, level) && !ItemBoostUtils.isBoosted(inv.getItem(0));
    }

    @NotNull
    @Override
    public List<ItemStack> rollResults(List<ProcessingOutput> rollableResults, @NotNull RandomSource randomSource) {
        List<ItemStack> results = new ArrayList<>();
        ProcessingOutput output = rollableResults.getFirst();
        ItemStack stack = output.rollOutput(randomSource);
        if (!stack.isEmpty()) {
            ItemBoostUtils.setBoosted(stack, true);
            results.add(stack);
        }
        return results;
    }

    @NotNull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
