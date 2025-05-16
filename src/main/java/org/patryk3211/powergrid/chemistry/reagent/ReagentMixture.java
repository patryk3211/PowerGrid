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
package org.patryk3211.powergrid.chemistry.reagent;

import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.chemistry.recipe.ReactionFlag;
import org.patryk3211.powergrid.chemistry.recipe.ReactionRecipe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class ReagentMixture {
    private final Map<Reagent, Integer> reagents = new HashMap<>();
    private boolean burning;
    private double energy;

    private double heatMass;
    private int totalAmount;

    public ReagentMixture() {
        totalAmount = 0;
        heatMass = 0;
        energy = 0;
        burning = false;
    }

    /**
     * Calculate stack heat energy.
     * @param temperature Temperature in Celsius
     * @param amount Amount in moles * 1000
     * @param reagent Reagent
     */
    private static double stackEnergy(double temperature, int amount, Reagent reagent) {
        return (temperature + 273.15) * stackHeatMass(amount, reagent);
    }

    private static double stackHeatMass(int amount, Reagent reagent) {
        return (amount * 0.001) * reagent.getHeatCapacity();
    }

    /**
     * Get temperature of the mixture
     * @return Temperature in Celsius
     */
    public float getTemperature() {
        return (float) Math.round(getTemperaturePrecise() * 100) / 100;
    }

    private double getTemperaturePrecise() {
        return (energy / heatMass) - 273.15;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public int getVolume() {
        return Integer.MAX_VALUE;
    }

    /**
     * Add a stack of reagent to the mixture.
     * @param stack Stack to be added
     * @return Amount actually added
     */
    public final int add(ReagentStack stack) {
        return addInternal(stack.getReagent(), stack.getAmount(), stack.getTemperature(), true);
    }

    public final int add(ReagentStack stack, TransactionContext transaction) {
        var maxTransfer = accepts(stack);
        TransactionCallback.onSuccess(transaction, () -> addInternal(stack.getReagent(), maxTransfer, stack.getTemperature(), true));
        return maxTransfer;
    }

    protected int addInternal(Reagent reagent, int amount, double temperature, boolean affectEnergy) {
        if(affectEnergy) {
            energy += stackEnergy(temperature, amount, reagent);
        }
        heatMass += stackHeatMass(amount, reagent);
        totalAmount += amount;
        reagents.compute(reagent, (key, currentAmount) -> currentAmount == null ? amount : currentAmount + amount);
        return amount;
    }

    public Set<Reagent> getReagents() {
        return reagents.keySet();
    }

    /**
     * Get the amount of reagent of the given type contained in this mixture.
     * @param reagent Reagent type
     * @return Amount of reagent in moles * 1000
     */
    public int getAmount(Reagent reagent) {
        var amount = reagents.get(reagent);
        return amount == null ? 0 : amount;
    }

    public ReagentState getState(Reagent reagent) {
        return reagent.properties.getState(getTemperature());
    }

    public boolean hasReagent(Reagent reagent) {
        return getAmount(reagent) > 0;
    }

    /**
     * Get the concentration of reagent of the given type in this mixture.
     * @param reagent Reagent type
     * @return Concentration in [0; 1] range
     */
    public float getConcentration(Reagent reagent) {
        var amount = getAmount(reagent);
        return (float) amount / totalAmount;
    }

    /**
     * Remove an amount of a specific reagent from this mixture.
     * @param reagent Reagent type
     * @param amount Amount of reagent to remove in moles * 1000
     * @return Removed reagent stack
     */
    public final ReagentStack remove(Reagent reagent, int amount) {
        var temperature = getTemperature();
        var removed = Math.min(getAmount(reagent), amount);
        removeInternal(reagent, removed, true);
        return new ReagentStack(reagent, removed, temperature);
    }

    public final ReagentStack remove(Reagent reagent, int amount, TransactionContext transaction) {
        var temperature = getTemperature();
        var removed = Math.min(getAmount(reagent), amount);
        TransactionCallback.onSuccess(transaction, () -> removeInternal(reagent, removed, true));
        return new ReagentStack(reagent, removed, temperature);
    }

    protected void removeInternal(Reagent reagent, int amount, boolean affectEnergy) {
        var invAmount = getAmount(reagent);
        if(invAmount == 0)
            return;
        if(amount >= invAmount) {
            amount = invAmount;
            reagents.remove(reagent);
        } else {
            reagents.put(reagent, invAmount - amount);
        }
        if(affectEnergy) {
            var temperature = getTemperaturePrecise();
            energy -= stackEnergy(temperature, amount, reagent);
        }
        heatMass -= stackHeatMass(amount, reagent);
        totalAmount -= amount;
    }

    /**
     * Remove a given amount of reagents from this mixture.
     * Specific reagent amounts are calculated so that the concentration in
     * the result mixture is preserved.
     * @param requestedAmount Amount to remove in moles * 1000
     * @return Removed reagent mixture
     */
    public ReagentMixture remove(int requestedAmount) {
        var extractedMixture = new ReagentMixture();
        int extractedAmount = 0;
        var temperature = getTemperaturePrecise();
        for(var entry : reagents.entrySet()) {
            float concentration = (float) entry.getValue() / totalAmount;
            int reagentAmount = Math.round(requestedAmount * concentration);
            var reagentHeatMass = stackHeatMass(reagentAmount, entry.getKey());
            var reagentEnergy = stackEnergy(temperature, reagentAmount, entry.getKey());
            energy -= reagentEnergy;
            heatMass -= reagentHeatMass;
            extractedAmount += requestedAmount;
            extractedMixture.energy += reagentEnergy;
            extractedMixture.heatMass += reagentHeatMass;
            extractedMixture.totalAmount += requestedAmount;
            extractedMixture.reagents.put(entry.getKey(), reagentAmount);
        }
        totalAmount -= extractedAmount;
        return extractedMixture;
    }

    /**
     * Calculates the amount of reagent that this mixture can accept from the given stack.
     * @return Accepted amount of reagent
     */
    public int accepts(ReagentStack stack) {
        return stack.getAmount();
    }

    public void addEnergy(float energy) {
        this.energy += energy;
    }

    public void removeEnergy(float energy) {
        addEnergy(-energy);
    }

    public float getEnergy() {
        return (float) energy;
    }

    public float getEnergy(float minTemperature) {
        var baseEnergy = (minTemperature + 273.15f) * heatMass;
        return (float) (energy - baseEnergy);
    }

    public boolean isBurning() {
        return burning;
    }

    public void setBurning(boolean burning) {
        this.burning = burning;
    }

    /**
     * Applies a given recipe to the mixture. This method applies the reaction at the maximum
     * possible rate given by the recipe and conditions inside the mixture.
     * Warning! This function assumes that the recipe was previously tested and all the required conditions match.
     * @param reaction Reaction recipe to apply to this mixture.
     */
    public void applyReaction(ReactionRecipe reaction) {
        // First calculate the reaction rate.
        int reactionRate = reaction.getReactionRate();
        for(var ingredient : reaction.getReagentIngredients()) {
            var amount = getAmount(ingredient.getReagent());
            reactionRate = Math.min(amount / ingredient.getRequiredAmount(), reactionRate);
        }
        // Then apply the reaction at the calculated rate.
        var temperature = getTemperaturePrecise();
        double ingredientEnergy = 0;
        for(var ingredient : reaction.getReagentIngredients()) {
            ingredientEnergy += stackEnergy(temperature, ingredient.getRequiredAmount() * reactionRate, ingredient.getReagent());
            removeInternal(ingredient.getReagent(), ingredient.getRequiredAmount() * reactionRate, true);
        }
        double resultHeatMass = 0;
        for(var result : reaction.getReagentResults()) {
            resultHeatMass += stackHeatMass(result.getAmount(), result.getReagent());
        }
        double resultEnergy = ingredientEnergy + reaction.getReactionEnergy() * reactionRate;
        for(var result : reaction.getReagentResults()) {
            addInternal(result.getReagent(), result.getAmount() * reactionRate, resultEnergy / resultHeatMass, true);
        }
        if(reaction.hasFlag(ReactionFlag.COMBUSTION)) {
            // If recipe is a combustion recipe we can set the burning flag so that other things burn too.
            setBurning(true);
        }
    }

    public void write(NbtCompound tag) {
        var reagentList = new NbtList();

        for(var entry : reagents.entrySet()) {
            var nbtEntry = new NbtCompound();
            var id = ReagentRegistry.REGISTRY.getId(entry.getKey());
            nbtEntry.putString("Id", id.toString());
            nbtEntry.putInt("Amount", entry.getValue());
            reagentList.add(nbtEntry);
        }

        tag.put("Reagents", reagentList);
        tag.putDouble("Energy", energy);
        if(burning) {
            tag.putBoolean("Burning", true);
        }
    }

    public void read(NbtCompound tag) {
        reagents.clear();
        totalAmount = 0;
        heatMass = 0;
        energy = tag.getFloat("Energy");
        var reagentList = tag.getList("Reagents", NbtElement.COMPOUND_TYPE);
        for(var entry : reagentList) {
            var obj = (NbtCompound) entry;
            var id = obj.getString("Id");
            var amount = obj.getInt("Amount");
            var reagent = ReagentRegistry.REGISTRY.get(new Identifier(id));
            reagents.put(reagent, amount);
            totalAmount += amount;
            heatMass += stackHeatMass(amount, reagent);
        }
        if(tag.contains("Burning")) {
            burning = tag.getBoolean("Burning");
        } else {
            burning = false;
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ReagentMixture(T=" + getTemperature() + ",reagents=[");
        boolean first = true;
        for(var entry : reagents.entrySet()) {
            if(first) {
                first = false;
            } else {
                str.append(",");
            }
            str.append(entry.getValue()).append(" ").append(entry.getKey());
        }
        return str + "])";
    }

    public FluidView getFluidView() {
        return new FluidView(this);
    }

    public static class FluidView implements Storage<FluidVariant> {
        private final ReagentMixture mixture;

        public FluidView(ReagentMixture mixture) {
            this.mixture = mixture;
        }

        @Override
        public long insert(FluidVariant fluid, long amount, TransactionContext transaction) {
            var reagent = Reagent.getReagent(fluid.getFluid());
            if(reagent == null)
                return 0;
            var stack = new ReagentStack(reagent, (int) (amount / Reagent.FLUID_MOLE_RATIO));
            if(fluid.hasNbt()) {
                var nbt = fluid.getNbt();
                if(nbt.contains("Temperature"))
                    stack.setTemperature(nbt.getFloat("Temperature"));
            }
            var added = mixture.add(stack, transaction);
            return amount * added / stack.getAmount();
        }

        @Override
        public long extract(FluidVariant fluid, long amount, TransactionContext transaction) {
            if(!fluid.hasNbt())
                return 0;
            var tag = fluid.getNbt();
            var temperature = tag.getFloat("Temperature");
            if(Math.round(temperature) != Math.round(mixture.getTemperature()))
                return 0;
            var reagent = Reagent.getReagent(fluid.getFluid());
            if(reagent == null)
                return 0;
            var stack = mixture.remove(reagent, (int) (amount / Reagent.FLUID_MOLE_RATIO), transaction);
            return (long) (stack.getAmount() * Reagent.FLUID_MOLE_RATIO);
        }

        @NotNull
        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            return mixture.getReagents().stream()
                    .filter(reagent -> mixture.getState(reagent) == ReagentState.LIQUID)
                    .map(reagent -> (StorageView<FluidVariant>) new SingleFluidView(mixture, reagent))
                    .iterator();
        }
    }

    public static class SingleFluidView implements StorageView<FluidVariant> {
        private final ReagentMixture mixture;
        private final Reagent reagent;
        private final FluidVariant resource;

        public SingleFluidView(ReagentMixture mixture, Reagent reagent) {
            this.mixture = mixture;
            this.reagent = reagent;
            var fluid = reagent.asFluid();
            if(fluid != null) {
                var tag = new NbtCompound();
                tag.putFloat("Temperature", mixture.getTemperature());
                this.resource = FluidVariant.of(fluid, tag);
            } else {
                this.resource = null;
            }
        }

        @Override
        public long extract(FluidVariant fluid, long amount, TransactionContext transaction) {
            if(Reagent.getReagent(fluid.getFluid()) != reagent)
                return 0;
            if(!fluid.hasNbt())
                return 0;
            if(Math.round(mixture.getTemperature()) != Math.round(fluid.getNbt().getFloat("Temperature")))
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
            return mixture.getAmount(reagent);
        }

        @Override
        public long getCapacity() {
            return mixture.getVolume();
        }
    }
}
