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
package org.patryk3211.powergrid.kinetics.punchcard.fabric;

import com.simibubi.create.foundation.item.ItemHelper;
import io.github.fabricators_of_create.porting_lib.transfer.ViewOnlyWrappedStorageView;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardItem;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardReaderBlock;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardReaderBlockEntity;

import java.util.Iterator;
import java.util.List;

public class PunchCardReaderBlockEntityImpl extends PunchCardReaderBlockEntity implements SidedStorageBlockEntity {
    private final ItemStackHandler inputInventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            progress.setValue(0);
            notifyUpdate();
        }
    };

    private final ItemStackHandler outputInventory = new ItemStackHandler(1);
    private final InventoryWrapper capability = new InventoryWrapper();

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
                var variant = inputInventory.getSlot(0).getResource();
                try(var ctx = Transaction.openOuter()) {
                    if(inputInventory.extract(variant, 1, ctx) != 0) {
                        if(outputInventory.insert(variant, 1, ctx) != 0) {
                            ctx.commit();
                        }
                    }
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
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inputInventory);
        ItemHelper.dropContents(level, worldPosition, outputInventory);
    }

    @Override
    public @Nullable Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        if(side == null || side == Direction.UP || side == getBlockState().getValue(PunchCardReaderBlock.HORIZONTAL_FACING)) {
            return capability;
        }
        return null;
    }

    private class InventoryWrapper extends CombinedStorage<ItemVariant, ItemStackHandler> {
        public InventoryWrapper() {
            super(List.of(inputInventory, outputInventory));
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if(resource.getItem() instanceof PunchCardItem)
                return inputInventory.insert(resource, maxAmount, transaction);
            return 0;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return outputInventory.extract(resource, maxAmount, transaction);
        }

        @Override
        public @NotNull Iterator<StorageView<ItemVariant>> iterator() {
            return new InventoryHandlerIterator();
        }

        private class InventoryHandlerIterator implements Iterator<StorageView<ItemVariant>> {
            private boolean output = true;
            private Iterator<StorageView<ItemVariant>> wrapped;

            public InventoryHandlerIterator() {
                wrapped = outputInventory.iterator();
            }

            @Override
            public boolean hasNext() {
                return wrapped.hasNext();
            }

            @Override
            public StorageView<ItemVariant> next() {
                StorageView<ItemVariant> view = wrapped.next();
                if (!output) view = new ViewOnlyWrappedStorageView<>(view);
                if (output && !hasNext()) {
                    wrapped = inputInventory.iterator();
                    output = false;
                }
                return view;
            }
        }
    }
}
