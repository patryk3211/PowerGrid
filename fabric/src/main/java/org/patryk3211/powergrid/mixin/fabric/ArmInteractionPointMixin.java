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
package org.patryk3211.powergrid.mixin.fabric;

import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.patryk3211.powergrid.circuits.circuitboard.IncompleteCircuitItem;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = ArmInteractionPoint.class, remap = false)
public abstract class ArmInteractionPointMixin {
    @Shadow @Nullable protected abstract Storage<ItemVariant> getHandler();

    @Shadow public abstract Level getLevel();

    @Shadow public abstract BlockPos getPos();

    @Unique
    private ItemStack powerGrid$handleDepot(ItemStack stack, TransactionContext ctx) {
        var handler = getHandler();
        if (handler == null)
            return null;
        // Try to insert into a circuit
        try(var inner = ctx.openNested()) {
            for(var view : handler) {
                if(view.isResourceBlank() || view.getAmount() == 0)
                    continue;
                var circuit = view.getResource();
                if(circuit.isOf(ModdedItems.INCOMPLETE_CIRCUIT.get())) {
                    if(handler.extract(view.getResource(), 1, ctx) == 0)
                        continue;
                    var newCircuit = IncompleteCircuitItem.insert(getLevel(), circuit.toStack(), stack);
                    if(newCircuit != null) {
                        if(handler.insert(ItemVariant.of(newCircuit), 1, ctx) == 1) {
                            var result = ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - 1);
                            inner.commit();
                            return result;
                        }
                    }
                    inner.abort();
                    return stack;
                }
            }
        }
        return null;
    }

    @Unique
    private ItemStack powerGrid$handleBelt(ItemStack stack, TransactionContext ctx) {
        var beltBE = BeltHelper.getSegmentBE(getLevel(), getPos());
        if (beltBE == null)
            return null;
        var transport = beltBE.getBehaviour(TransportedItemStackHandlerBehaviour.TYPE);
        if (transport == null)
            return null;
        var found = new MutableBoolean(false);
        var inserted = new MutableBoolean(false);
        transport.handleCenteredProcessingOnAllItems(0.05f, tis -> {
            if(found.isFalse() && ModdedItems.INCOMPLETE_CIRCUIT.isIn(tis.stack)) {
                found.setTrue();
                var newCircuit = IncompleteCircuitItem.insert(getLevel(), tis.stack, stack);
                if(newCircuit != null) {
                    inserted.setTrue();
                    TransactionCallback.onSuccess(ctx, () -> {
                        var result = new TransportedItemStack(newCircuit);
                        result.lockedExternally = newCircuit.is(ModdedItems.INCOMPLETE_CIRCUIT.get());
                        transport.handleProcessingOnItem(tis,
                                TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(List.of(), result)
                        );
                    });
                }
            }
            return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
        });
        if(found.isFalse())
            return null;
        if(inserted.isFalse())
            return stack;
        return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - 1);
    }

    @Inject(
            method = "insert",
            at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/api/transfer/v1/storage/Storage;insert(Ljava/lang/Object;JLnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)J"),
            cancellable = true
    )
    private void powerGrid$insertAssembleCircuit(ItemStack stack, TransactionContext ctx, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack remainder = null;
        if(((Object) this) instanceof AllArmInteractionPointTypes.BeltPoint) {
            remainder = powerGrid$handleBelt(stack, ctx);
        } else if(((Object) this) instanceof AllArmInteractionPointTypes.DepotPoint) {
            remainder = powerGrid$handleDepot(stack, ctx);
        }
        if(remainder != null)
            cir.setReturnValue(remainder);
    }
}
