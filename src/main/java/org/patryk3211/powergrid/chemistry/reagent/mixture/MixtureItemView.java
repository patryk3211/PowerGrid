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

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;

import java.util.Iterator;

@SuppressWarnings("UnstableApiUsage")
public class MixtureItemView implements Storage<ItemVariant> {
    private final ReagentMixture mixture;

    public MixtureItemView(ReagentMixture mixture) {
        this.mixture = mixture;
    }

    @Override
    public long insert(ItemVariant item, long amount, TransactionContext transaction) {
        var reagent = Reagent.getReagent(item.getItem());
        if(reagent == null)
            return 0;
        var stack = new ReagentStack(reagent, (int) (amount * reagent.getItemAmount()));
        if(item.hasNbt()) {
            var nbt = item.getNbt();
            if(nbt.contains("Temperature"))
                stack.setTemperature(nbt.getFloat("Temperature"));
        }
        // Calculate accepted item count.
        int added;
        try(var inner = transaction.openNested()) {
            added = mixture.add(stack, inner);
            inner.abort();
        }
        int itemCount = added / reagent.getItemAmount();
        stack.setAmount(itemCount * reagent.getItemAmount());
        // Add accepted reagent.
        mixture.add(stack, transaction);
        return itemCount;
    }

    @Override
    public long extract(ItemVariant item, long amount, TransactionContext transaction) {
        if(!item.hasNbt())
            return 0;
        var tag = item.getNbt();
        var temperature = tag.getFloat("Temperature");
        if(Math.round(temperature) != Math.round(mixture.getTemperature()))
            return 0;
        var reagent = Reagent.getReagent(item.getItem());
        if(reagent == null)
            return 0;

        int removed;
        try(var inner = transaction.openNested()) {
            var stack = mixture.remove(reagent, (int) (amount * reagent.getItemAmount()), inner);
            removed = stack.getAmount() / reagent.getItemAmount();
            inner.abort();
        }
        mixture.remove(reagent, removed * reagent.getItemAmount(), transaction);
        return removed;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return mixture.getReagents().stream()
                .filter(reagent -> mixture.getState(reagent) == ReagentState.SOLID)
                .map(reagent -> (StorageView<ItemVariant>) new MixtureSingleItemView(mixture, reagent))
                .iterator();
    }
}
