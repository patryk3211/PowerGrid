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

import org.ejml.data.DMatrixRMaj;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import static org.patryk3211.powergrid.electricity.sim.special.PNJunction.gm;

public class TransistorNPN extends DynamicConductanceWire {
    private final PNJunction beJunction;
    private final float bias;
    private final float beta;
    private final float earlyVoltage;

    private double Ice;
    private double Ibe;

    public TransistorNPN(IElectricNode collector, IElectricNode emitter, IElectricNode base, float bias, float beta, float earlyVoltage) {
        super(collector, emitter);
        this.bias = bias;
        this.beta = beta;
        this.earlyVoltage = earlyVoltage;
        this.beJunction = new PNJunction(base, emitter);
    }

    @Override
    public void setNetwork(ElectricalNetwork network) {
        super.setNetwork(network);
        if(network != null) {
            network.addWire(beJunction);
        } else {
            beJunction.remove();
            beJunction.setNetwork(null);
        }
    }

    @Override
    protected double calculateConductance() {
        double V_BE = beJunction.getLimitedPotential();
        double V_CE = potentialDifference();

        if(V_CE > 0) {
            // Removing Early effect for g_m makes the simulation potentially faster
            var gm = gm(V_BE, (1 + V_CE / earlyVoltage) * 10, bias);
            var I_C = gm * V_BE;

            var gpi = gm / beta;
            beJunction.updateConductance(gpi);

            var go = I_C / earlyVoltage;
            Ice = I_C - gm * V_BE - go * V_CE;
            Ibe = I_C / beta - gpi * V_BE;
            return go;
        } else {
            // No reverse active
            var gm = gm(V_BE, 10, bias);
            var I_C = gm * V_BE;

            var gpi = gm / beta;
            beJunction.updateConductance(gpi);
            Ice = 0;
            Ibe = I_C / beta - gpi * V_BE;
            return 0;
        }
    }

    @Override
    public void addResidual(DMatrixRMaj residual) {
        residual.add(node1.getIndex(), 0, Ice);
        residual.add(beJunction.getNode1().getIndex(), 0, Ibe);
        residual.add(node2.getIndex(), 0, -Ibe - Ice);
    }

    @Override
    public float current() {
        return (float) (super.current() - Ice + beJunction.current());
    }

    @Override
    public float power() {
        return (float) (potentialDifference() * (super.current() - Ice) + beJunction.power());
    }
}
