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

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.nbt.NbtCompound;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;

@SuppressWarnings("UnstableApiUsage")
public class MixtureSingleFluidView implements StorageView<FluidVariant> {
    private final ReagentMixture mixture;
    private final Reagent reagent;
    private final FluidVariant resource;

    public MixtureSingleFluidView(ReagentMixture mixture, Reagent reagent) {
        this.mixture = mixture;
        this.reagent = reagent;
        var fluid = reagent.asFluid();
        if(fluid != null) {
            this.resource = FluidVariant.of(fluid, null);
        } else {
            this.resource = null;
        }
    }

    @Override
    public long extract(FluidVariant fluid, long amount, TransactionContext transaction) {
        if(Reagent.getReagent(fluid.getFluid()) != reagent)
            return 0;
        var removedStack = mixture.remove(reagent, (int) Math.ceil(amount / Reagent.FLUID_MOLE_RATIO), transaction);
        return (long) (removedStack.getAmount() * Reagent.FLUID_MOLE_RATIO);
    }

    @Override
    public boolean isResourceBlank() {
        return resource == null;
    }

    @Override
    public FluidVariant getResource() {
        return resource;
    }

    @Override
    public long getAmount() {
        return (long) (mixture.getAmount(reagent) * Reagent.FLUID_MOLE_RATIO);
    }

    @Override
    public long getCapacity() {
        return (long) (mixture.getVolume() * Reagent.FLUID_MOLE_RATIO);
    }
}
