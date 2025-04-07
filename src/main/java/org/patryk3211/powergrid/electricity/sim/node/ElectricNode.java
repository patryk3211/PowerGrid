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

import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;

public abstract class ElectricNode implements IElectricNode {
    private int matrixIndex;

    protected float voltage;
    protected float current;

    protected ElectricalNetwork network;

    public ElectricNode() {
        voltage = 0;
        current = 0;
    }

    @Override
    public void setNetwork(ElectricalNetwork network) {
        this.network = network;
    }

    @Override
    public ElectricalNetwork getNetwork() {
        return network;
    }

    public void assignIndex(int index) {
        matrixIndex = index;
    }

    public int getIndex() {
        return matrixIndex;
    }

    @Override
    public float getVoltage() {
        return voltage;
    }

    @Override
    public float getCurrent() {
        return current;
    }

    public void setVoltage(float voltage) {
        this.voltage = voltage;
    }

    public void setCurrent(float current) {
        this.current = current;
    }
}
