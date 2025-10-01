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
package org.patryk3211.powergrid.electricity.sim.node;

import org.ejml.data.DMatrixRMaj;

import java.util.Collection;
import java.util.List;

public class VoltageSourceCoupling extends CouplingNode {
    private final IElectricNode positive;
    private final IElectricNode negative;
    private float voltage;
    private float resistance;

    public VoltageSourceCoupling(IElectricNode positive, IElectricNode negative, float resistance) {
        this.positive = positive;
        this.negative = negative;
        this.resistance = resistance;
    }

    public VoltageSourceCoupling(IElectricNode positive, IElectricNode negative, Float resistance) {
        this.positive = positive;
        this.negative = negative;
        this.resistance = resistance;
    }

    public VoltageSourceCoupling(IElectricNode positive, IElectricNode negative, float resistance, float voltage) {
        this(positive, negative, resistance);
        setVoltage(voltage);
    }

    public void setVoltage(float voltage) {
        if(network != null)
            network.updateCurrentMatrix(this, voltage - this.voltage);
        this.voltage = voltage;
    }

    public void setResistance(float resistance) {
        if(network != null)
            network.alterConductanceMatrix(this.index, this.index, -(resistance - this.resistance));
        this.resistance = resistance;
    }

    @Override
    public void couple(DMatrixRMaj conductance) {
        conductance.add(this.index, positive.getIndex(),  1);
        conductance.add(this.index, negative.getIndex(), -1);
        conductance.add(positive.getIndex(), this.index,  1);
        conductance.add(negative.getIndex(), this.index, -1);
        conductance.add(this.index, this.index, -resistance);
    }

    @Override
    public Collection<IElectricNode> coupledNodes() {
        return List.of(positive, negative);
    }

    public float getCurrent() {
        return (float) getStateValue();
    }

    public float getVoltage() {
        return voltage;
    }

    public float getResistance() {
        return resistance;
    }

    public IElectricNode getPositive() {
        return positive;
    }

    public IElectricNode getNegative() {
        return negative;
    }
}
