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
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;

import java.util.Iterator;

@SuppressWarnings("UnstableApiUsage")
public class MixtureFluidView implements Storage<FluidVariant> {
    private final ReagentMixture mixture;

    public MixtureFluidView(ReagentMixture mixture) {
        this.mixture = mixture;
    }

    @Override
    public long insert(FluidVariant fluid, long amount, TransactionContext transaction) {
        var reagent = Reagent.getReagent(fluid.getFluid());
        if(reagent == null)
            return 0;
        var stack = new ReagentStack(reagent, (int) (amount / Reagent.FLUID_MOLE_RATIO), reagent.getFluidTemperature());
        var added = mixture.add(stack, transaction);
        TransactionCallback.onSuccess(transaction, mixture::setAltered);
        return amount * added / stack.getAmount();
    }

    @Override
    public long extract(FluidVariant fluid, long amount, TransactionContext transaction) {
        var reagent = Reagent.getReagent(fluid.getFluid());
        if(reagent == null)
            return 0;
        var stack = mixture.remove(reagent, (int) Math.ceil(amount / Reagent.FLUID_MOLE_RATIO), transaction);
        TransactionCallback.onSuccess(transaction, mixture::setAltered);
        return (long) (stack.getAmount() * Reagent.FLUID_MOLE_RATIO);
    }

    @NotNull
    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        return mixture.getReagents().stream()
                .filter(reagent -> mixture.getState(reagent) == ReagentState.LIQUID)
                .map(reagent -> (StorageView<FluidVariant>) new MixtureSingleFluidView(mixture, reagent))
                .iterator();
    }
}
