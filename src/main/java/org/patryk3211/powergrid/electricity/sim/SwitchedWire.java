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
package org.patryk3211.powergrid.electricity.sim;

import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.G_MIN;

public class SwitchedWire extends ElectricWire {
    private static final double OFF_CONDUCTANCE = G_MIN * 0.5;

    private boolean state;
    private int stamp;

    public SwitchedWire(float resistance, IElectricNode node1, IElectricNode node2) {
        super(resistance, node1, node2);
        state = true;
    }

    public SwitchedWire(float resistance, IElectricNode node1, IElectricNode node2, boolean initialState) {
        super(resistance, node1, node2);
        state = initialState;
    }

    public void setState(boolean state) {
        if(this.state != state) {
            this.state = state;
            if(network != null) {
                if(state) {
                    // Switch is now on, add its conductance
                    network.updateConductance(this, super.conductance() - OFF_CONDUCTANCE);
                } else {
                    // Switch is now off, remove its conductance
                    network.updateConductance(this, -super.conductance() + OFF_CONDUCTANCE);
                }
                stamp = network.getStamp();
            }
        }
    }

    @Override
    public boolean isConverged() {
        return network != null && network.getStamp() != stamp;
    }

    public boolean getState() {
        return this.state;
    }

    @Override
    public void setResistance(double resistance) {
        if(state) {
            super.setResistance(resistance);
        } else {
            // If switch is off we don't update the conductance matrix since it is zero anyway.
            this.resistance = resistance;
        }
    }

    @Override
    public double current() {
        return state ? super.current() : 0;
    }

    @Override
    public double conductance() {
        return state ? super.conductance() : OFF_CONDUCTANCE;
    }

    @Override
    public String toString() {
        return String.format("SwitchedWire(R=%g,%s)", resistance, state ? "ON" : "OFF");
    }
}
