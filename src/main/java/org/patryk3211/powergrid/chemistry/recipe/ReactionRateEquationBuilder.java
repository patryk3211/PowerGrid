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
package org.patryk3211.powergrid.chemistry.recipe;

import org.patryk3211.powergrid.chemistry.reagent.ReagentConvertible;
import org.patryk3211.powergrid.chemistry.recipe.equation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ReactionRateEquationBuilder<P extends ReactionRateEquationBuilder.Parent> {
    private final P parent;
    protected final List<IReactionEquation> elements = new ArrayList<>();

    public ReactionRateEquationBuilder(P parent) {
        this.parent = parent;
    }

    public P build() {
        if(parent == null)
            throw new IllegalStateException("Cannot build if parent is null");
        parent.use(this);
        return parent;
    }

    private List<IReactionEquation> remap(List<IReactionEquation> equations) {
        var remapped = new ArrayList<IReactionEquation>();
        for(var eq : equations) {
            if(eq instanceof AggregateEquation) {
                remapped.add(new MapAggregateEquation(List.of(eq)));
            } else {
                remapped.add(eq);
            }
        }
        return remapped;
    }

    protected List<IReactionEquation> remapped() {
        return remap(elements);
    }

    public ReactionRateEquationBuilder<P> aggregateEquation(UnaryOperator<ReactionRateEquationBuilder<?>> builder, Function<List<IReactionEquation>, ? extends AggregateEquation> constructor) {
        var part = builder.apply(new ReactionRateEquationBuilder<>(null));
        this.elements.add(constructor.apply(part.remapped()));
        return this;
    }

    public ReactionRateEquationBuilder<P> add(UnaryOperator<ReactionRateEquationBuilder<?>> elements) {
        return aggregateEquation(elements, AddEquation::new);
    }

    public ReactionRateEquationBuilder<P> subtract(UnaryOperator<ReactionRateEquationBuilder<?>> elements) {
        return aggregateEquation(elements, SubtractEquation::new);
    }

    public ReactionRateEquationBuilder<P> multiply(UnaryOperator<ReactionRateEquationBuilder<?>> elements) {
        return aggregateEquation(elements, MultiplyEquation::new);
    }

    public ReactionRateEquationBuilder<P> divide(UnaryOperator<ReactionRateEquationBuilder<?>> elements) {
        return aggregateEquation(elements, DivideEquation::new);
    }

    public ReactionRateEquationBuilder<P> min(UnaryOperator<ReactionRateEquationBuilder<?>> elements) {
        return aggregateEquation(elements, MinEquation::new);
    }

    public ReactionRateEquationBuilder<P> max(UnaryOperator<ReactionRateEquationBuilder<?>> elements) {
        return aggregateEquation(elements, MaxEquation::new);
    }

    public ReactionRateEquationBuilder<P> polynomial(UnaryOperator<ReactionRateEquationBuilder<?>> elements) {
        return aggregateEquation(elements, PolynomialEquation::new);
    }

    public ReactionRateEquationBuilder<P> number(float number) {
        var eq = new ConstEquation(number);
        this.elements.add(eq);
        return this;
    }

    public ReactionRateEquationBuilder<P> temperature() {
        this.elements.add(new TemperatureEquation());
        return this;
    }

    public ReactionRateEquationBuilder<P> concentration(ReagentConvertible reagent) {
        this.elements.add(new ConcentrationEquation(reagent.asReagent()));
        return this;
    }

    public ReactionRateEquationBuilder<P> catalyzer() {
        this.elements.add(new CatalyzerEquation());
        return this;
    }

    public interface Parent {
        void use(ReactionRateEquationBuilder<?> child);
    }
}
