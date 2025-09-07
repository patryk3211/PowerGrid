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
package org.patryk3211.powergrid.electricity.sim.special;

import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

class PNJunction extends AbstractElectricWire {
    public static final double V_T = 0.05;
    private double currentConductance;
    private double prevV;

    public PNJunction(IElectricNode base, IElectricNode emitter) {
        super(base, emitter);
    }

    public static double pnlim(double V, double Vprev) {
        if(V >= Vprev) {
            var step = 0.25f;
            V = (float) (Vprev + step * Math.log(1 + (V - Vprev) / step));
        }
        return V;
    }

    public static double gm(double V, double G_max, double V_bias) {
        return 0.5 * (Math.tanh((V - V_bias - 0.3) / (2 * V_T)) + 1) * G_max;
    }

    public void updateConductance(double newConductance) {
        network.updateConductance(this, newConductance - currentConductance);
        currentConductance = newConductance;
    }

    @Override
    public double conductance() {
        return currentConductance;
    }

    // This should be called once per iteration for correct smoothing.
    public double getLimitedPotential() {
        var V = pnlim(potentialDifference(), prevV);
        prevV = V;
        return V;
    }
}
