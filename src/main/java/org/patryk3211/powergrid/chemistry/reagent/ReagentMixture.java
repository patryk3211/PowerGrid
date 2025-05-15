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

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.chemistry.recipe.ReactionFlag;
import org.patryk3211.powergrid.chemistry.recipe.ReactionRecipe;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ReagentMixture {
    private final Map<Reagent, Integer> reagents = new HashMap<>();
    private boolean burning;
    private float energy;

    private float heatMass;
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
    private static float stackEnergy(float temperature, int amount, Reagent reagent) {
        return (temperature + 273.15f) * stackHeatMass(amount, reagent);
    }

    private static float stackHeatMass(int amount, Reagent reagent) {
        return (amount * 0.001f) * reagent.getHeatCapacity();
    }

    /**
     * Get temperature of the mixture
     * @return Temperature in Celsius
     */
    public float getTemperature() {
        return (energy / heatMass) - 273.15f;
    }

    /**
     * Add a stack of reagent to the mixture.
     * @param stack Stack to be added
     * @return Amount actually added
     */
    public final int add(ReagentStack stack) {
        return addInternal(stack.getReagent(), stack.getAmount(), stack.getTemperature(), true);
    }

    protected final int addInternal(Reagent reagent, int amount) {
        return addInternal(reagent, amount, 0, false);
    }

    protected int addInternal(Reagent reagent, int amount, float temperature, boolean affectEnergy) {
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
        var removed = removeInternal(reagent, amount, true);
        return new ReagentStack(reagent, removed, temperature);
    }

    protected int removeInternal(Reagent reagent, int amount, boolean affectEnergy) {
        var invAmount = getAmount(reagent);
        if(invAmount == 0)
            return 0;
        if(amount >= invAmount) {
            amount = invAmount;
            reagents.remove(reagent);
        } else {
            reagents.put(reagent, invAmount - amount);
        }
        if(affectEnergy) {
            var temperature = getTemperature();
            energy -= stackEnergy(temperature, amount, reagent);
        }
        heatMass -= stackHeatMass(amount, reagent);
        totalAmount -= amount;
        return amount;
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
        var temperature = getTemperature();
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
        return energy;
    }

    public float getEnergy(float minTemperature) {
        var baseEnergy = (minTemperature + 273.15f) * heatMass;
        return energy - baseEnergy;
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
        var temperature = getTemperature();
        float ingredientEnergy = 0;
        for(var ingredient : reaction.getReagentIngredients()) {
            ingredientEnergy += stackEnergy(temperature, ingredient.getRequiredAmount() * reactionRate, ingredient.getReagent());
            removeInternal(ingredient.getReagent(), ingredient.getRequiredAmount() * reactionRate, true);
        }
        float resultHeatMass = 0;
        for(var result : reaction.getReagentResults()) {
            resultHeatMass += stackHeatMass(result.getAmount(), result.getReagent());
        }
        float resultEnergy = ingredientEnergy + reaction.getReactionEnergy() * reactionRate;
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
        tag.putFloat("Energy", energy);
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
}
