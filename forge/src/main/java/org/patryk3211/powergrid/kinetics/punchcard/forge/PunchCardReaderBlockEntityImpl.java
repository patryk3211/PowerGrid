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

import com.simibubi.create.foundation.item.ItemHelper;
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
    private final ItemStackHandler inventory = new ItemStackHandler(1);

    private final LazyOptional<IItemHandler> sideCapability = LazyOptional.of(() -> new InventoryWrapper(false));
    private final LazyOptional<IItemHandler> topCapability = LazyOptional.of(() -> new InventoryWrapper(true));

    public PunchCardReaderBlockEntityImpl(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public ItemStack currentItem() {
        return inventory.getStackInSlot(0);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        sideCapability.invalidate();
        topCapability.invalidate();
    }

    @Override
    public void dropItems() {
        ItemHelper.dropContents(level, worldPosition, inventory);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        inventory.deserializeNBT(compound.getCompound("Inv"));
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("Inv", inventory.serializeNBT());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            if(side == null || side == Direction.UP) {
                return topCapability.cast();
            } else if(side == getBlockState().getValue(PunchCardReaderBlock.HORIZONTAL_FACING)) {
                return sideCapability.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public boolean insertCard(ItemStack stack, Direction side) {
        stack = stack.copyWithCount(1);
        if(!currentItem().isEmpty())
            return false;
        inventory.insertItem(0, stack, false);
        if(side == Direction.UP) {
            prevAngle = angle = 0;
        } else {
            prevAngle = angle = 16;
        }
        notifyUpdate();
        return true;
    }

    @Override
    public ItemStack extractCard() {
        var extracted = inventory.extractItem(0, 1, false);
        if(!extracted.isEmpty())
            notifyUpdate();
        return extracted;
    }

    private class InventoryWrapper extends CombinedInvWrapper {
        private final boolean top;

        public InventoryWrapper(boolean top) {
            super(inventory);
            this.top = top;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof PunchCardItem && super.isItemValid(slot, stack);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if(!isItemValid(slot, stack))
                return stack;
            var current = getStackInSlot(slot);
            if(!current.isEmpty())
                return stack;
            var remaining = super.insertItem(slot, stack.copyWithCount(1), simulate);
            if(remaining.isEmpty() && !simulate) {
                remaining = stack.copyWithCount(stack.getCount() - 1);
                prevAngle = angle = top ? 0 : 16;
                notifyUpdate();
            }
            return remaining;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if(top && angle > 0)
                return ItemStack.EMPTY;
            if(!top && angle < 16)
                return ItemStack.EMPTY;
            var extracted = super.extractItem(slot, amount, simulate);
            if(!simulate)
                notifyUpdate();
            return extracted;
        }
    }
}
