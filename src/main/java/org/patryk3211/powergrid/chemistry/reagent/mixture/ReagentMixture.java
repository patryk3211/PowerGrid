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

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.electrolysis.ElectrolysisRecipe;
import org.patryk3211.powergrid.chemistry.reagent.*;
import org.patryk3211.powergrid.chemistry.recipe.ReagentConditions;
import org.patryk3211.powergrid.chemistry.recipe.ReactionFlag;
import org.patryk3211.powergrid.chemistry.recipe.ReactionRecipe;
import org.patryk3211.powergrid.chemistry.recipe.RecipeProgressStore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReagentMixture extends SnapshotParticipant<MixtureSnapshot> implements ReagentConditions {
    protected final Map<Reagent, Integer> reagents = new HashMap<>();
    private boolean burning;
    protected double energy;

    private double heatMass;
    private int totalAmount;

    private float catalyzer;

    private MixtureFluidView fluidView;
    private MixtureItemView itemView;

    private boolean altered;

    public ReagentMixture() {
        totalAmount = 0;
        heatMass = 0;
        energy = 0;
        catalyzer = 0;
        burning = false;
    }

    @Override
    protected MixtureSnapshot createSnapshot() {
        return new MixtureSnapshot(this);
    }

    @Override
    protected void readSnapshot(MixtureSnapshot mixtureSnapshot) {
        reagents.clear();
        totalAmount = 0;
        heatMass = 0;
        energy = mixtureSnapshot.getEnergy();
        for(var entry : mixtureSnapshot.getReagents().entrySet()) {
            var reagent = entry.getKey();
            var amount = entry.getValue();
            reagents.put(reagent, amount);
            totalAmount += amount;
            heatMass += stackHeatMass(amount, reagent);
        }
    }

    @Override
    protected void onFinalCommit() {
        altered = true;
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
    @Override
    public float temperature() {
        return (float) Math.round(getTemperaturePrecise() * 100) / 100;
    }

    public double getTemperaturePrecise() {
        return getAbsoluteTemperature() - 273.15;
    }
    
    public double getAbsoluteTemperature() {
        if(heatMass() == 0)
            return 0;
        return energy / heatMass();
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    @Override
    public double heatMass() {
        return heatMass;
    }

    public int getVolume() {
        return Integer.MAX_VALUE;
    }

    public void setCatalyzer(float catalyzer) {
        this.catalyzer = catalyzer;
    }

    /**
     * Add a stack of reagent to the mixture.
     * @param stack Stack to be added
     * @param transaction Transaction context
     * @return Amount actually added
     */
    public final int add(ReagentStack stack, TransactionContext transaction) {
        var maxTransfer = accepts(stack);
        updateSnapshots(transaction);

        return addInternal(stack.getReagent(), maxTransfer, stack.getTemperature(), true);
    }

    protected int addInternal(Reagent reagent, int amount, double temperature, boolean affectEnergy) {
        if(amount == 0 || reagent == Reagents.EMPTY)
            return 0;
        if(affectEnergy) {
            energy += stackEnergy(temperature, amount, reagent);
            energyChanged();
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
        return amount == null ? 0 : Math.max(amount, 0);
    }

    public ReagentState getState(Reagent reagent) {
        return reagent.getState(temperature());
    }

    public boolean hasReagent(Reagent reagent) {
        return getAmount(reagent) > 0;
    }

    /**
     * Get the concentration of reagent of the given type in this mixture.
     * @param reagent Reagent type
     * @return Concentration in [0; 1] range
     */
    @Override
    public float concentration(ReagentConvertible reagent) {
        var amount = getAmount(reagent.asReagent());
        return (float) amount / totalAmount;
    }

    @Override
    public float concentration(ReagentConvertible reagent, ReagentState state) {
        if(getState(reagent.asReagent()) != state)
            return 0;

        var total = 0;
        for(var key : reagents.keySet()) {
            if(getState(key) != state)
                continue;
            total += getAmount(key);
        }

        return (float) getAmount(reagent.asReagent()) / total;
    }

    @Override
    public float catalyzer() {
        return catalyzer;
    }

    /**
     * Remove an amount of a specific reagent from this mixture.
     * @param reagent Reagent type
     * @param amount Amount of reagent to remove in moles * 1000
     * @param transaction Transaction context
     * @return Removed reagent stack
     */
    public final ReagentStack remove(Reagent reagent, int amount, TransactionContext transaction) {
        var temperature = temperature();

        updateSnapshots(transaction);

        int actual = removeInternal(reagent, amount, true);
        return new ReagentStack(reagent, actual, temperature);
    }

    protected int removeInternal(Reagent reagent, int amount, boolean affectEnergy) {
        var invAmount = getAmount(reagent);
        if(invAmount == 0)
            return 0;
        if(amount >= invAmount) {
            if(invAmount < 0)
                invAmount = 0;
            amount = invAmount;
            reagents.remove(reagent);
        } else {
            reagents.put(reagent, invAmount - amount);
        }
        if(affectEnergy) {
            var temperature = getTemperaturePrecise();
            energy -= stackEnergy(temperature, amount, reagent);
            energyChanged();
        }
        heatMass -= stackHeatMass(amount, reagent);
        totalAmount -= amount;
        return amount;
    }

    /**
     * Add a mixture of reagents to this mixture.
     * @param mixture Mixture to add
     * @param transaction Transaction context
     * @return Total added amount
     */
    public int add(ReagentMixture mixture, TransactionContext transaction) {
        updateSnapshots(transaction);

        int total = 0;
        for(var entry : mixture.reagents.entrySet()) {
            if(entry.getValue() <= 0)
                continue;
            var stack = new ReagentStack(entry.getKey(), entry.getValue(), mixture.temperature());
            total += addInternal(stack.getReagent(), stack.getAmount(), stack.getTemperature(), true);
        }
        return total;
    }

    /**
     * Remove a given amount of reagents from this mixture.
     * Specific reagent amounts are calculated so that the concentration in
     * the result mixture is preserved.
     * @param requestedAmount Amount to remove in moles * 1000
     * @param reagents Set of reagents to extract
     * @param transaction Transaction context
     * @return Removed reagent mixture
     */
    public ReagentMixture remove(int requestedAmount, Set<Reagent> reagents, TransactionContext transaction) {
        int total = 0;
        for(var reagent : reagents) {
            total += getAmount(reagent);
        }
        if(total < requestedAmount)
            requestedAmount = total;

        updateSnapshots(transaction);

        var temperature = temperature();
        var extractedMixture = new ReagentMixture();
        int extractedAmount = 0;
        for(var reagent : reagents) {
            double concentration = (double) getAmount(reagent) / total;
            int reagentAmount = Math.min(Math.max((int) Math.round(requestedAmount * concentration), 1), requestedAmount - extractedAmount);
            var removed = removeInternal(reagent, reagentAmount, true);
            extractedMixture.addInternal(reagent, removed, temperature, true);

            extractedAmount += reagentAmount;
        }
        return extractedMixture;
    }

    public ReagentMixture remove(int requestedAmount, ReagentState state, TransactionContext transaction) {
        var reagents = new HashSet<Reagent>();
        for(var reagent : this.reagents.keySet()) {
            if(getState(reagent) != state)
                continue;
            reagents.add(reagent);
        }
        return remove(requestedAmount, reagents, transaction);
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
        energyChanged();
    }

    public void removeEnergy(float energy) {
        addEnergy(-energy);
    }

    protected void energyChanged() { }

    public float getEnergy() {
        return (float) energy;
    }

    public float getEnergy(float minTemperature) {
        var baseEnergy = (minTemperature + 273.15f) * heatMass();
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
    public void applyReaction(ReactionRecipe reaction, RecipeProgressStore progressStore) {
        // First calculate the reaction rate.
        float reactionRate = reaction.calculateRate(this, progressStore.getProgress(reaction));
        if(reactionRate <= 0)
            return;
        for(var ingredient : reaction.getReagentIngredients()) {
            var amount = getAmount(ingredient.getReagent());
            reactionRate = Math.min(amount / ingredient.getRequiredAmount(), reactionRate);
        }
        var reactionEnergy = reaction.getReactionEnergy();
        var temperature = getTemperaturePrecise();
        if(reactionEnergy > 0) {
            // Check energy remaining to max temperature
        }

        // Then apply the reaction at the calculated rate.
        double ingredientEnergy = 0;
        int quantReactionRate = (int) reactionRate;
        for(var ingredient : reaction.getReagentIngredients()) {
            ingredientEnergy += stackEnergy(temperature, ingredient.getRequiredAmount() * quantReactionRate, ingredient.getReagent());
            removeInternal(ingredient.getReagent(), ingredient.getRequiredAmount() * quantReactionRate, true);
        }
        double resultHeatMass = 0;
        for(var result : reaction.getReagentResults()) {
            resultHeatMass += stackHeatMass(result.getAmount() * quantReactionRate, result.getReagent());
        }
        double resultEnergy = ingredientEnergy + reactionEnergy * quantReactionRate;
        for(var result : reaction.getReagentResults()) {
            addInternal(result.getReagent(), result.getAmount() * quantReactionRate, resultEnergy / resultHeatMass, true);
        }
        if(reaction.hasFlag(ReactionFlag.COMBUSTION)) {
            // If recipe is a combustion recipe we can set the burning flag so that other things burn too.
            setBurning(true);
        }
        float remainder = reactionRate - quantReactionRate;
        progressStore.setProgress(reaction, remainder);
    }

    public int applyReaction(ElectrolysisRecipe recipe, float current, ReagentMixture negativeReceiver) {
        // TODO: Maybe add progress store here as well.
        var rate = (int) (current * 2);
        for(var ingredient : recipe.getReagentIngredients()) {
            var amount = getAmount(ingredient.getReagent());
            rate = Math.min(amount / ingredient.getRequiredAmount(), rate);
        }

        var temperature = getTemperaturePrecise();
        double ingredientEnergy = 0;
        for(var ingredient : recipe.getReagentIngredients()) {
            ingredientEnergy += stackEnergy(temperature, ingredient.getRequiredAmount() * rate, ingredient.getReagent());
            removeInternal(ingredient.getReagent(), ingredient.getRequiredAmount() * rate, true);
        }

        double resultHeatMass = 0;
        for(var result : recipe.getReagentResults()) {
            var stack = result.reagent();
            resultHeatMass += stackHeatMass(stack.getAmount() * rate, stack.getReagent());
        }
        for(var result : recipe.getReagentResults()) {
            var receiver = result.negative() ? negativeReceiver : this;
            var stack = result.reagent();
            receiver.addInternal(stack.getReagent(), stack.getAmount() * rate, ingredientEnergy / resultHeatMass, true);
        }
        return rate;
    }

    public ReagentMixture scaledBy(float scale) {
        var result = new ReagentMixture();
        for(var entry : reagents.entrySet()) {
            var amount = (int) (entry.getValue() * scale);
            result.reagents.put(entry.getKey(), amount);
            result.totalAmount += amount;
            result.heatMass += stackHeatMass(amount, entry.getKey());
        }
        result.energy = (getTemperaturePrecise() + 273.15) * result.heatMass;
        return result;
    }

    public ReagentMixture scaledTo(int size) {
        return scaledBy((float) size / totalAmount);
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
        energyChanged();
    }

    public void read(NbtCompound tag) {
        reagents.clear();
        totalAmount = 0;
        heatMass = 0;
        energy = tag.getDouble("Energy");
        var reagentList = tag.getList("Reagents", NbtElement.COMPOUND_TYPE);
        for(var entry : reagentList) {
            var obj = (NbtCompound) entry;
            var id = obj.getString("Id");
            var amount = obj.getInt("Amount");
            var reagent = ReagentRegistry.REGISTRY.get(new Identifier(id));
            if(reagent != null) {
                reagents.put(reagent, amount);
                totalAmount += amount;
                heatMass += stackHeatMass(amount, reagent);
            } else {
                PowerGrid.LOGGER.warn("Invalid reagent id in mixture nbt: '{}'", id);
            }
        }
        if(tag.contains("Burning")) {
            burning = tag.getBoolean("Burning");
        } else {
            burning = false;
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ReagentMixture(T=" + temperature() + ",reagents=[");
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

    public MixtureFluidView getFluidView() {
        if(fluidView == null)
            fluidView = new MixtureFluidView(this);
        return fluidView;
    }

    public MixtureItemView getItemView() {
        if(itemView == null)
            itemView = new MixtureItemView(this);
        return itemView;
    }

    public void setAltered() {
        this.altered = true;
    }

    public boolean wasAltered() {
        boolean state = altered;
        altered = false;
        return state;
    }
}
