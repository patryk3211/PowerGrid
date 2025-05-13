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
import org.patryk3211.powergrid.chemistry.recipe.ReactionRecipe;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ReagentMixture {
    private final Map<Reagent, Integer> reagents = new HashMap<>();
    private int totalAmount;

    private float heatMass;
    private float energy;

    public ReagentMixture() {
        totalAmount = 0;
        heatMass = 0;
        energy = 0;
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

    public void add(ReagentStack stack) {
        energy += stackEnergy(stack.getTemperature(), stack.getAmount(), stack.getReagent());
        heatMass += stackHeatMass(stack.getAmount(), stack.getReagent());
        totalAmount += stack.getAmount();
        reagents.compute(stack.getReagent(), (reagent, currentAmount) -> currentAmount == null ? stack.getAmount() : currentAmount + stack.getAmount());
    }

    protected void add(ReagentStack stack, float temperature, int rate) {
        var amount = stack.getAmount() * rate;
        energy += stackEnergy(temperature, amount, stack.getReagent());
        heatMass += stackHeatMass(amount, stack.getReagent());
        totalAmount += amount;
        reagents.compute(stack.getReagent(), (reagent, currentAmount) -> currentAmount == null ? amount : currentAmount + amount);
    }

    public Set<Reagent> getReagents() {
        return reagents.keySet();
    }

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

    public float getConcentration(Reagent reagent) {
        var amount = reagents.get(reagent);
        if(amount == null)
            return 0;
        return (float) amount / totalAmount;
    }

    public ReagentStack remove(Reagent reagent, int amount) {
        var invAmount = getAmount(reagent);
        if(invAmount == 0)
            return ReagentStack.EMPTY;
        if(amount >= invAmount) {
            amount = invAmount;
            reagents.remove(reagent);
        } else {
            reagents.put(reagent, invAmount - amount);
        }
        var temperature = getTemperature();
        energy -= stackEnergy(temperature, amount, reagent);
        heatMass -= stackHeatMass(amount, reagent);
        totalAmount -= amount;
        return new ReagentStack(reagent, amount, temperature);
    }

    protected void remove(ReagentIngredient ingredient, int rate) {
        var reagent = ingredient.getReagent();
        var amount = ingredient.getRequiredAmount() * rate;

        var invAmount = getAmount(reagent);
        if(invAmount == 0)
            return;
        if(amount >= invAmount) {
            amount = invAmount;
            reagents.remove(reagent);
        } else {
            reagents.put(reagent, invAmount - amount);
        }
        var temperature = getTemperature();
        energy -= stackEnergy(temperature, amount, reagent);
        heatMass -= stackHeatMass(amount, reagent);
        totalAmount -= amount;
    }

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

    /**
     * Warning! This function assumes that the recipe was previously tested and all the required conditions match.
     * @param reaction Reaction recipe to apply to this mixture.
     */
    public void applyReaction(ReactionRecipe reaction) {
        var temperature = getTemperature();
        // First calculate the reaction rate.
        int reactionRate = reaction.getReactionRate();
        for(var ingredient : reaction.getReagentIngredients()) {
            var amount = getAmount(ingredient.getReagent());
            reactionRate = Math.min(amount / ingredient.getRequiredAmount(), reactionRate);
        }
        // Then apply the reaction at the calculated rate.
        for(var ingredient : reaction.getReagentIngredients()) {
            remove(ingredient, reactionRate);
        }
        for(var result : reaction.getReagentResults()) {
            add(result, temperature, reactionRate);
        }
        addEnergy(reaction.getReactionEnergy() * reactionRate);
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
