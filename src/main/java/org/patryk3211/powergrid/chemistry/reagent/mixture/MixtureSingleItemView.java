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
package org.patryk3211.powergrid.chemistry.reagent.mixture;

import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;

@SuppressWarnings("UnstableApiUsage")
public class MixtureSingleItemView implements StorageView<ItemVariant> {
    private final ReagentMixture mixture;
    private final Reagent reagent;
    private final ItemVariant resource;

    public MixtureSingleItemView(ReagentMixture mixture, Reagent reagent) {
        this.mixture = mixture;
        this.reagent = reagent;
        var item = reagent.asItem();
        if (item != null) {
            this.resource = ItemVariant.of(item, null);
        } else {
            this.resource = null;
        }
    }

    @Override
    public long extract(ItemVariant item, long amount, TransactionContext transaction) {
        if(Reagent.getReagent(item.getItem()) != reagent)
            return 0;

        int itemCount;
        try(var inner = transaction.openNested()) {
            var removedStack = mixture.remove(reagent, (int) (amount * reagent.getItemAmount()), inner);
            itemCount = removedStack.getAmount() / reagent.getItemAmount();
            inner.abort();
        }
        mixture.remove(reagent, itemCount * reagent.getItemAmount(), transaction);
        TransactionCallback.onSuccess(transaction, mixture::setAltered);
        return itemCount;
    }

    @Override
    public boolean isResourceBlank() {
        return resource == null;
    }

    @Override
    public ItemVariant getResource() {
        return resource;
    }

    @Override
    public long getAmount() {
        return mixture.getAmount(reagent) / reagent.getItemAmount();
    }

    @Override
    public long getCapacity() {
        return mixture.getVolume() / reagent.getItemAmount();
    }
}
