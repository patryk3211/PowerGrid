/**
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
 **/
package org.patryk3211.electricity;

public class ElectricWire {
    private float resistance;
    public final IElectricNode node1;
    public final IElectricNode node2;

    public ElectricWire(float resistance, IElectricNode node1, IElectricNode node2) {
        this.resistance = resistance;
        this.node1 = node1;
        this.node2 = node2;
    }

    public void setResistance(float resistance) {
        this.resistance = resistance;
    }

    public float potentialDifference() {
        return Math.abs(node1.getVoltage() - node2.getVoltage());
    }

    public float current() {
        return potentialDifference() / resistance;
    }

    public float conductance() {
        return 1 / resistance;
    }
}
