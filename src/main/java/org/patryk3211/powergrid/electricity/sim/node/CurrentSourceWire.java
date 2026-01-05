/*
 * Copyright 2026 patryk3211
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
package org.patryk3211.powergrid.electricity.sim.node;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IStaticResidual;

public class CurrentSourceWire extends AbstractElectricWire implements IStaticResidual {
    private double current;
    private double conductance;

    public CurrentSourceWire(IElectricNode positive, @Nullable IElectricNode negative, double conductance) {
        super(positive, negative);
        this.conductance = conductance;
    }

    public CurrentSourceWire(IElectricNode positive, @Nullable IElectricNode negative, Number conductance) {
        super(positive, negative);
        this.conductance = conductance.doubleValue();
    }

    public void setConductance(double conductance) {
        if(network != null) {
            network.updateConductance(this, conductance - this.conductance);
        }
        this.conductance = conductance;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    @Override
    public double conductance() {
        return conductance;
    }

    @Override
    public boolean isSource() {
        return true;
    }

    @Override
    public void addStaticResidual(IResidualAdder residual) {
        residual.add(node1.getIndex(), current);
        if(node2 != null)
            residual.add(node2.getIndex(), -current);
    }

    public float getResistance() {
        return (float) (1 / conductance);
    }

    public double getCurrent() {
        return current;
    }
}
