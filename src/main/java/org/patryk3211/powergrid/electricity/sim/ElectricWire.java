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

public class ElectricWire extends AbstractElectricWire {
    protected double resistance;

    public ElectricWire(double resistance, IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
        validateResistance(resistance);
        this.resistance = resistance;
    }

    public void setResistance(double resistance) {
        validateResistance(resistance);
        double old = this.resistance;
        this.resistance = resistance;
        if(network != null)
            this.network.updateResistance(this, old);
    }

    public double getResistance() {
        return resistance;
    }

    @Override
    public double conductance() {
        validateResistance(resistance);
        return 1 / resistance;
    }

    private static void validateResistance(double resistance) {
        if(resistance <= 0)
            throw new IllegalArgumentException("Wire resistance must be greater than zero");
        if(!Double.isFinite(resistance))
            throw new IllegalArgumentException("Wire resistance is not finite");
    }
}
