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

import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.item.ItemStack;
import org.patryk3211.powergrid.circuits.circuitboard.IncompleteCircuitItem;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ArmInteractionPoint.class)
public abstract class ArmInteractionPointMixin {
    @Shadow(remap = false) @Nullable protected abstract Storage<ItemVariant> getHandler();

    @Inject(
            method = "insert(Lnet/minecraft/item/ItemStack;Lnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)Lnet/minecraft/item/ItemStack;",
            at = @At("TAIL"),
            cancellable = true
    )
    private void insertAssembleCircuit(ItemStack stack, TransactionContext ctx, CallbackInfoReturnable<ItemStack> cir) {
        if(!(((Object) this) instanceof AllArmInteractionPointTypes.DepotPoint))
            return;
        var handler = getHandler();
        if (handler == null)
            return;
        var remainder = cir.getReturnValue();
        if(remainder.isEmpty())
            return;
        // Try to insert into a circuit
        try(var inner = ctx.openNested()) {
            for(var view : handler) {
                if(view.isResourceBlank() || view.getAmount() == 0)
                    continue;
                var circuit = view.getResource();
                if(circuit.isOf(ModdedItems.INCOMPLETE_CIRCUIT.get())) {
                    if(view.extract(view.getResource(), 1, ctx) == 0)
                        continue;
                    var newCircuit = IncompleteCircuitItem.insert(circuit, remainder);
                    if(newCircuit != null) {
                        if(handler.insert(newCircuit, 1, ctx) == 1) {
                            remainder.decrement(1);
                            inner.commit();
                        }
                    }
                }
            }
        }
    }
}
