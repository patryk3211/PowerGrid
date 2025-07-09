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

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Recipe;
import net.minecraft.util.collection.DefaultedList;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BasinRecipe.class)
public class BasinRecipeMixin {
    @Inject(
            method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/recipe/Recipe;Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;acceptOutputs(Ljava/util/List;Ljava/util/List;Lnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)Z"
            )
    )
    private static void applyTransferNbt(BasinBlockEntity basin, Recipe<?> recipe, boolean test, CallbackInfoReturnable<Boolean> cir, @Local DefaultedList<ItemStack> consumedItems, @Local(ordinal = 0) List<ItemStack> outputs) {
        NbtCompound schematic = null;
        for(var consumed : consumedItems) {
            if(consumed.isIn(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag) && consumed.hasNbt()) {
                var root = consumed.getNbt();
                if(root.contains("Schematic")) {
                    schematic = consumed.getNbt().getCompound("Schematic").copy();
                }
            }
        }
        if(schematic == null)
            return;
        for(var output : outputs) {
            if(output.isIn(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)) {
                var tag = new NbtCompound();
                tag.put("Schematic", schematic);
                output.setNbt(tag);
            }
        }
    }
}
