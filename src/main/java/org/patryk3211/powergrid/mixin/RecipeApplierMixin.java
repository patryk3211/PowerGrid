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
package org.patryk3211.powergrid.mixin;

import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeApplier.class)
public class RecipeApplierMixin {
    @Inject(
            method = "applyRecipeOn(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/recipe/Recipe;)Ljava/util/List;",
            at = @At("RETURN")
    )
    private static void recipeTransferNbt(World level, ItemStack stackIn, Recipe<?> recipe, CallbackInfoReturnable<List<ItemStack>> cir) {
        var outputs = cir.getReturnValue();
        if(outputs.isEmpty() || !stackIn.isIn(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag) || !stackIn.hasNbt() || !stackIn.getNbt().contains("Schematic"))
            return;
        // Modify output with NBT
        for(var output : outputs) {
            if(output.isIn(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)) {
                var schematic = stackIn.getNbt().getCompound("Schematic").copy();
                output.getOrCreateNbt().put("Schematic", schematic);
            }
        }
    }
}
