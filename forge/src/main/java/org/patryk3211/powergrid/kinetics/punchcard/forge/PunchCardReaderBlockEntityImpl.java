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
package org.patryk3211.powergrid.kinetics.punchcard.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardItem;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardReaderBlock;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardReaderBlockEntity;

public class PunchCardReaderBlockEntityImpl extends PunchCardReaderBlockEntity {
    private final ItemStackHandler inputInventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            progress.setValue(0);
            notifyUpdate();
        }
    };

    private final ItemStackHandler outputInventory = new ItemStackHandler(1);
    private final LazyOptional<IItemHandler> capability = LazyOptional.of(InventoryWrapper::new);

    public PunchCardReaderBlockEntityImpl(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public ItemStack currentItem() {
        return inputInventory.getStackInSlot(0);
    }

    @Override
    public void tick() {
        super.tick();
        if(progress.getValue() >= 1) {
            if(!inputInventory.getStackInSlot(0).isEmpty()) {
                var stack = inputInventory.extractItem(0, 1, true);
                if(!stack.isEmpty() && outputInventory.insertItem(0, stack, false).isEmpty()) {
                    inputInventory.extractItem(0, 1, false);
                }
            }
        }
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        inputInventory.deserializeNBT(compound.getCompound("Input"));
        outputInventory.deserializeNBT(compound.getCompound("Output"));
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("Input", inputInventory.serializeNBT());
        compound.put("Output", outputInventory.serializeNBT());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            if(side == null || side == Direction.UP || side == getBlockState().getValue(PunchCardReaderBlock.HORIZONTAL_FACING)) {
                return capability.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    private class InventoryWrapper extends CombinedInvWrapper {
        public InventoryWrapper() {
            super(inputInventory, outputInventory);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (outputInventory == getHandlerFromIndex(getIndexForSlot(slot)))
                return false;
            return stack.getItem() instanceof PunchCardItem && super.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (outputInventory == getHandlerFromIndex(getIndexForSlot(slot)))
                return stack;
            if (!isItemValid(slot, stack))
                return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (inputInventory == getHandlerFromIndex(getIndexForSlot(slot)))
                return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    }
}
