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
package org.patryk3211.powergrid.chemistry.recipe.equation;

import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentRegistry;
import org.patryk3211.powergrid.chemistry.recipe.ReagentConditions;

public class ConcentrationEquation implements IReactionEquation {
    public static final Type<IReactionEquation> TYPE = new Type<>("Conc", VarEquation.CODEC);

    private final Reagent reagent;

    public ConcentrationEquation(String reagentId) {
        reagent = ReagentRegistry.REGISTRY.get(new Identifier(reagentId));
    }

    public ConcentrationEquation(Reagent reagent) {
        this.reagent = reagent;
    }

    public String getArg() {
        return ReagentRegistry.REGISTRY.getId(reagent).toString();
    }

    @Override
    public float evaluate(ReagentConditions conditions) {
        return conditions.concentration(reagent);
    }

    @Override
    public Type<?> getType() {
        return TYPE;
    }
}
