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
package org.patryk3211.powergrid.mixin.forge;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.items.IItemHandler;
import org.patryk3211.powergrid.utility.RecipeNbtTransfer;
import org.patryk3211.powergrid.utility.forge.IterateItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = BasinRecipe.class, remap = false)
public class BasinRecipeMixin {
    @Inject(
            method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;acceptOutputs(Ljava/util/List;Ljava/util/List;Z)Z"
            )
    )
    private static void powerGrid$applyTransferNbtForge(BasinBlockEntity basin, Recipe<?> recipe, boolean test, CallbackInfoReturnable<Boolean> cir, @Local IItemHandler availableItems, @Local(ordinal = 0) List<ItemStack> outputs) {
        RecipeNbtTransfer.transfer(IterateItemHandler.iterable(availableItems), outputs);
    }
}
