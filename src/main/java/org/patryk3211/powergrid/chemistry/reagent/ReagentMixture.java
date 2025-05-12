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

import java.util.HashMap;
import java.util.Map;

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

    public float getTemperature() {
        return energy / heatMass;
    }

    public void add(ReagentStack stack) {
        var stackEnergy = stack.getTemperature() * stack.getAmount() * stack.getReagent().getHeatCapacity();
        energy += stackEnergy;
        heatMass += stack.getAmount() * stack.getReagent().getHeatCapacity();
        totalAmount += stack.getAmount();
        reagents.compute(stack.getReagent(), (reagent, currentAmount) -> currentAmount == null ? stack.getAmount() : currentAmount + stack.getAmount());
    }

    public int getAmount(Reagent reagent) {
        var amount = reagents.get(reagent);
        return amount == null ? 0 : amount;
    }

    public ReagentStack remove(Reagent reagent, int amount) {
        var invAmount = getAmount(reagent);
        if(amount > invAmount) {
            amount = invAmount;
        }
        var temperature = getTemperature();
        energy -= temperature * amount * reagent.getHeatCapacity();
        heatMass -= amount * reagent.getHeatCapacity();
        totalAmount -= amount;
        return new ReagentStack(reagent, amount, temperature);
    }

    public ReagentMixture remove(int requestedAmount) {
        var extractedMixture = new ReagentMixture();
        int extractedAmount = 0;
        var temperature = getTemperature();
        for(var entry : reagents.entrySet()) {
            float concentration = (float) entry.getValue() / totalAmount;
            int reagentAmount = (int) (requestedAmount * concentration);
            var reagentHeatMass = reagentAmount * entry.getKey().getHeatCapacity();
            var reagentEnergy = temperature * reagentHeatMass;
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
}
