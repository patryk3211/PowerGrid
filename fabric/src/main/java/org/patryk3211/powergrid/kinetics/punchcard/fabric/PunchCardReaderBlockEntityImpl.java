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
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
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
    private final ItemStackHandler inventory = new ItemStackHandler(1);

    private final InventoryWrapper sideCapability = new InventoryWrapper(false);
    private final InventoryWrapper topCapability = new InventoryWrapper(true);

    public PunchCardReaderBlockEntityImpl(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public ItemStack currentItem() {
        return inventory.getStackInSlot(0);
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
    public void dropItems() {
        ItemHelper.dropContents(level, worldPosition, inventory);
    }

    @Override
    public boolean insertCard(ItemStack stack, Direction side) {
        if(!currentItem().isEmpty())
            return false;
        stack = stack.copyWithCount(1);
        if(TransferUtil.insertItem(inventory, stack) == 0)
            return false;
        if(side == Direction.UP) {
            progress.setValue(0);
        } else {
            progress.setValue(1);
        }
        notifyUpdate();
        return true;
    }

    @Override
    public ItemStack extractCard() {
        var extracted = TransferUtil.extractAnyItem(inventory, 1);
        if(!extracted.isEmpty())
            notifyUpdate();
        return extracted;
    }

    @Override
    public @Nullable Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        if(side == null || side == Direction.UP) {
            return topCapability;
        } else if(side == getBlockState().getValue(PunchCardReaderBlock.HORIZONTAL_FACING)) {
            return sideCapability;
        }
        return null;
    }

    private class InventoryWrapper extends CombinedStorage<ItemVariant, ItemStackHandler> {
        private final boolean top;

        public InventoryWrapper(boolean top) {
            super(List.of(inventory));
            this.top = top;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if(resource.getItem() instanceof PunchCardItem && inventory.empty()) {
                var inserted = inventory.insert(resource, maxAmount, transaction);
                if(inserted == 1) {
                    TransactionCallback.onSuccess(transaction, () -> {
                        progress.setValue(top ? 0 : 1);
                        notifyUpdate();
                    });
                }
                return inserted;
            }
            return 0;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if(top && progress.getValue() > 0)
                return 0;
            if(!top && progress.getValue() < 1)
                return 0;
            var extracted = inventory.extract(resource, maxAmount, transaction);
            if(extracted > 0) {
                TransactionCallback.onSuccess(transaction, PunchCardReaderBlockEntityImpl.this::notifyUpdate);
            }
            return extracted;
        }

        @Override
        public @NotNull Iterator<StorageView<ItemVariant>> iterator() {
            return inventory.iterator();
        }
    }
}
