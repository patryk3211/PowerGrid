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
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;
import org.patryk3211.powergrid.chemistry.reagent.Reagents;

public class ConstantReagentMixture extends ReagentMixture {
    public static final ConstantReagentMixture ATMOSPHERE = new ConstantReagentMixture(
            22.0f,
            new ReagentStack(Reagents.NITROGEN, 780),
            new ReagentStack(Reagents.OXYGEN, 210)
    );

    public ConstantReagentMixture(ReagentStack... reagents) {
        for(var stack : reagents) {
            super.addInternal(stack.getReagent(), stack.getAmount(), stack.getTemperature(), true);
        }
    }

    public ConstantReagentMixture(float temperature, ReagentStack... reagents) {
        this(reagents);
        energy = (temperature + 273.15) * heatMass();
    }

    @Override
    protected int removeInternal(Reagent reagent, int amount, boolean affectEnergy) {
        return 0;
    }

    @Override
    protected int addInternal(Reagent reagent, int amount, double temperature, boolean affectEnergy) {
        return 0;
    }

    @Override
    public int accepts(ReagentStack stack) {
        return 0;
    }
}
