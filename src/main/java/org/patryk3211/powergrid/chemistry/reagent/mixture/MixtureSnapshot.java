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

import org.patryk3211.powergrid.chemistry.reagent.Reagent;

import java.util.HashMap;
import java.util.Map;

public class MixtureSnapshot {
    private final Map<Reagent, Integer> reagents = new HashMap<>();
    private final double energy;

    public MixtureSnapshot(ReagentMixture mixture) {
        this.energy = mixture.energy;
        reagents.putAll(mixture.reagents);
    }

    public Map<Reagent, Integer> getReagents() {
        return reagents;
    }

    public double getEnergy() {
        return energy;
    }
}
