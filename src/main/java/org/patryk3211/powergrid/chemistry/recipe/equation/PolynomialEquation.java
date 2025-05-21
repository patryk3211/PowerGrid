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

import com.mojang.serialization.Codec;
import org.patryk3211.powergrid.chemistry.recipe.ReagentConditions;

import java.util.List;

public class PolynomialEquation extends AggregateEquation {
    public static final Codec<PolynomialEquation> CODEC = AggregateEquation.CODEC.xmap(PolynomialEquation::new, PolynomialEquation::getEquations);
    public static final Type<PolynomialEquation> TYPE = new Type<>("polynomial", CODEC);

    /**
     * n-th degree polynomial must contain at least three parameters, in which case it
     * specifies a linear equation. The first equation is always x, next equations specify
     * the coefficients starting from the largest term.
     */
    public PolynomialEquation(List<IReactionEquation> equations) {
        super(equations);
        if(equations.size() < 3)
            throw new IllegalArgumentException("Polynomial equation must contain at least three parameters");
    }

    @Override
    public float evaluate(ReagentConditions conditions) {
        float x = equations.get(0).evaluate(conditions);

        double result = 0, power = 1;
        for(int i = equations.size() - 1; i >= 1; --i) {
            result += power * equations.get(i).evaluate(conditions);
            power *= x;
        }
        return (float) result;
    }

    @Override
    public Type<?> getType() {
        return TYPE;
    }
}
