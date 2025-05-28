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

import org.patryk3211.powergrid.chemistry.GasConstants;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentConvertible;
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;

public class VolumeReagentInventory extends ReagentMixture {
    private final int volume;
    private int usedVolume;
    private int solidVolume;
    private int gasAmount;
    private boolean isOpen;

    public VolumeReagentInventory(int volume) {
        this.volume = volume;
        isOpen = false;
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
    }

    public final int getFreeVolume() {
        return volume - usedVolume;
    }

    @Override
    public final int getVolume() {
        return volume;
    }

    public float getFillLevel() {
        return (float) usedVolume / volume;
    }

    public float getSolidLevel() {
        return (float) solidVolume / volume;
    }

    public int getGasAmount() {
        return gasAmount;
    }

    public int getLiquidAmount() {
        return usedVolume - solidVolume;
    }

    public int getSolidAmount() {
        return solidVolume;
    }

    @Override
    protected void energyChanged() {
        super.energyChanged();
        refreshUsedVolume();
    }

    protected void refreshUsedVolume() {
        usedVolume = 0;
        solidVolume = 0;
        gasAmount = 0;
        for(var entry : reagents.entrySet()) {
            var state = getState(entry.getKey());
            if(state == ReagentState.GAS) {
                gasAmount += entry.getValue();
                continue;
            }
            usedVolume += entry.getValue();
            if(state == ReagentState.SOLID)
                solidVolume += entry.getValue();
        }
    }

    protected int accepts(Reagent reagent, int amount) {
        if(getState(reagent) == ReagentState.GAS) {
            // If there is space above the liquid level we accept any amount of gas.
            if(usedVolume < volume || isOpen)
                return amount;
            return 0;
        }
        return Math.min(amount, getFreeVolume());
    }

    @Override
    protected int addInternal(Reagent reagent, int amount, double temperature, boolean affectEnergy) {
        var result = super.addInternal(reagent, accepts(reagent, amount), temperature, affectEnergy);
        refreshUsedVolume();
        return result;
    }

    @Override
    protected int removeInternal(Reagent reagent, int amount, boolean affectEnergy) {
        var result = super.removeInternal(reagent, amount, affectEnergy);
        refreshUsedVolume();
        return result;
    }

    @Override
    public float concentration(ReagentConvertible reagent, ReagentState state) {
        if(getState(reagent.asReagent()) != state)
            return 0;
        var total = switch(state) {
            case GAS -> getGasAmount();
            case LIQUID -> getLiquidAmount();
            case SOLID -> getSolidAmount();
        };
        return (float) getAmount(reagent.asReagent()) / total;
    }

    public double staticPressure() {
        var volume = getFreeVolume();
        if(volume == 0)
            return 0;
        return (double) gasAmount * GasConstants.GAS_CONSTANT * getAbsoluteTemperature() / volume;
//        var volume = getFreeVolume() * 0.001f;
//        if(volume == 0)
//            return 0;
//        return getGasAmount() * 0.001 * GasConstants.GAS_CONSTANT * getAbsoluteTemperature() / volume;
    }

    public int nFromPressure(double p) {
        var volume = getFreeVolume() * 0.001f;
        if(volume == 0)
            return 0;
        return (int) (p * volume / (GasConstants.GAS_CONSTANT * getAbsoluteTemperature()) * 1000);
    }

    @Override
    public int accepts(ReagentStack stack) {
        return accepts(stack.getReagent(), stack.getAmount());
    }
}
