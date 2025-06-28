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
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.item.ItemStack;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(ArmBlockEntity.class)
public class ArmBlockEntityMixin {
    @Inject(method = "depositItem()V", at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint;insert(Lnet/minecraft/item/ItemStack;Lnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)Lnet/minecraft/item/ItemStack;"))
    private void depositItemMixin(CallbackInfo ci, @Local ArmInteractionPoint armInteractionPoint, @Local ItemStack remainder, @Local Transaction transaction) {
        if(!(armInteractionPoint instanceof AllArmInteractionPointTypes.DepotPoint))
            return;
//        var handler = ((ArmInteractionPointMixin) armInteractionPoint).getHandlerInvoke();
//        try(var inner = transaction.openNested()) {
//            Predicate<ItemVariant> variantPredicate = variant -> variant.isOf(ModdedItems.ETCHED_CIRCUIT.get()) || variant.isOf(ModdedItems.INCOMPLETE_CIRCUIT.get());
//            var circuit = TransferUtil.extractMatching(handler, variantPredicate, 1, inner);
//            if(circuit == null || circuit.amount() == 0)
//                return;
//        }
//        handler.iterator().forEachRemaining(view -> {
//            var stack = view.getResource();
//            stack.
//        });
//        armInteractionPoint.extract()
    }
}
