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

public class VolumeReagentInventory extends ReagentMixture {
    private final int volume;

    public VolumeReagentInventory(int volume) {
        this.volume = volume;
    }

    public final int getFreeVolume() {
        return volume - getTotalAmount();
    }

    @Override
    public final int getVolume() {
        return volume;
    }

    @Override
    protected int addInternal(Reagent reagent, int amount, double temperature, boolean affectEnergy) {
        return super.addInternal(reagent, Math.min(amount, getFreeVolume()), temperature, affectEnergy);
    }

    @Override
    public int accepts(ReagentStack stack) {
        return Math.min(getFreeVolume(), super.accepts(stack));
    }
}
