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

public class OpenReagentInventory extends ReagentMixture {
    public static final int ATMOSPHERIC_MOLES = 10000;
    public static final int ATMOSPHERIC_OXYGEN = (int) (ATMOSPHERIC_MOLES * 0.21f);
    public static final int ATMOSPHERIC_NITROGEN = (int) (ATMOSPHERIC_MOLES * 0.78f);

    public OpenReagentInventory() {

    }

    public void tick() {
        var reagents = getReagents();
        for(var reagent : reagents) {
            // TODO: This should be implemented as a slow process of the gas escaping into the atmosphere.
            if(getState(reagent) == ReagentState.GAS) {
                // Remove all reagent in gas form.
                super.removeInternal(reagent, getAmount(reagent), true);
            }
        }
    }

    @Override
    public int getAmount(Reagent reagent) {
        if(getState(reagent) == ReagentState.GAS) {
            if (reagent == Reagents.OXYGEN)
                return ATMOSPHERIC_OXYGEN;
            if (reagent == Reagents.NITROGEN)
                return ATMOSPHERIC_NITROGEN;
        }
        return super.getAmount(reagent);
    }

    @Override
    protected int addInternal(Reagent reagent, int amount, float temperature, boolean affectEnergy) {
        if(getState(reagent) == ReagentState.GAS) {
            // Reagent is "added" but it escapes into the "atmosphere".
            return amount;
        }
        return super.addInternal(reagent, amount, temperature, affectEnergy);
    }

    @Override
    protected int removeInternal(Reagent reagent, int amount, boolean affectEnergy) {
        if(getState(reagent) == ReagentState.GAS) {
            // Only oxygen and nitrogen are available in gas form.
            if(reagent == Reagents.OXYGEN || reagent == Reagents.NITROGEN)
                return amount;
            return 0;
        }
        return super.removeInternal(reagent, amount, affectEnergy);
    }
}
