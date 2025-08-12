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

import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
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

@Mixin(value = ArmInteractionPoint.class, remap = false)
public abstract class ArmInteractionPointMixin {
    @Shadow @Nullable protected abstract IItemHandler getHandler();

    @Shadow public abstract World getLevel();

    @Shadow public abstract BlockPos getPos();

    @Unique
    private ItemStack powerGrid$handleDepot(ItemStack componentStack, boolean simulate) {
        var handler = getHandler();
        if (handler == null)
            return null;
        // Try to insert into a circuit
        for(int i = 0; i < handler.getSlots(); ++i) {
            var circuitStack = handler.getStackInSlot(i);
            if(circuitStack.isEmpty())
                continue;
            if(circuitStack.isOf(ModdedItems.INCOMPLETE_CIRCUIT.get())) {
                var newCircuit = IncompleteCircuitItem.insert(circuitStack, componentStack);
                if(newCircuit != null) {
                    if(!simulate) {
                        var extracted = handler.extractItem(i, 1, false);
                        if(handler.insertItem(i, newCircuit, true).isEmpty()) {
                            // Commit
                            handler.insertItem(i, newCircuit, false);
                        } else {
                            // Abort
                            handler.insertItem(i, extracted, false);
                            return componentStack;
                        }
                    }
                    return ItemHandlerHelper.copyStackWithSize(componentStack, componentStack.getCount() - 1);
                }
                return componentStack;
            }
        }
        return null;
    }

    @Unique
    private ItemStack powerGrid$handleBelt(ItemStack stack, boolean simulate) {
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
                var newCircuit = IncompleteCircuitItem.insert(tis.stack, stack);
                if(newCircuit != null) {
                    inserted.setTrue();
                    if(!simulate) {
                        var result = new TransportedItemStack(newCircuit);
                        result.lockedExternally = newCircuit.isOf(ModdedItems.INCOMPLETE_CIRCUIT.get());
                        transport.handleProcessingOnItem(tis,
                                TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(List.of(), result)
                        );
                    }
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
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/ItemHandlerHelper;insertItem(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/item/ItemStack;"),
            cancellable = true
    )
    private void insertAssembleCircuit(ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack remainder = null;
        if(((Object) this) instanceof AllArmInteractionPointTypes.BeltPoint) {
            remainder = powerGrid$handleBelt(stack, simulate);
        } else if(((Object) this) instanceof AllArmInteractionPointTypes.DepotPoint) {
            remainder = powerGrid$handleDepot(stack, simulate);
        }
        if(remainder != null)
            cir.setReturnValue(remainder);
    }
}
