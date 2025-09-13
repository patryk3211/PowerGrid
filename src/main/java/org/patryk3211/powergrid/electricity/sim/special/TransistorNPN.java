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
    private final PNJunction bcJunction;
    private final float bias;
    private final float alpha;
    private final float beta;
    private final float earlyVoltage;

    private double Ice;
    private double Ibe;
    private double Ibc;

    private double prevIe;
    private double prevIc;

    public TransistorNPN(IElectricNode collector, IElectricNode emitter, IElectricNode base, float bias, float beta, float earlyVoltage) {
        super(collector, emitter);
        this.bias = bias;
        this.alpha = beta / (1 + beta);
        this.beta = beta * 2;
        this.earlyVoltage = earlyVoltage;
        this.beJunction = new PNJunction(base, emitter);
        this.bcJunction = new PNJunction(base, collector);
    }

    @Override
    public void setNetwork(ElectricalNetwork network) {
        super.setNetwork(network);
        if(network != null) {
            network.addWire(beJunction);
            network.addWire(bcJunction);
        } else {
            beJunction.remove();
            beJunction.setNetwork(null);
            bcJunction.remove();
            bcJunction.setNetwork(null);
        }
    }

    @Override
    protected double calculateConductance() {
        // Forward mode
        double V_BE = beJunction.getLimitedPotential();
        var Ie = PNJunction.gm(V_BE, 1, bias);
        double Ge;
        if(Math.abs(beJunction.dV) > 1e-5) {
            Ge = (Ie - prevIe) / beJunction.dV;
        } else {
            Ge = beJunction.conductance();
        }
        Ibe = Ie - Ge * V_BE;
        prevIe = Ie;
        beJunction.updateConductance(Ge + 1e-6);

        // Reverse mode
        double V_BC = bcJunction.getLimitedPotential();
        var Ic = PNJunction.gm(V_BC, 1, bias);
        double Gc;
        if(Math.abs(bcJunction.dV) > 1e-5) {
            Gc = (Ic - prevIc) / bcJunction.dV;
        } else {
            Gc = bcJunction.conductance();
        }
        Ibc = Ic - Gc * V_BC;
        prevIc = Ic;
        bcJunction.updateConductance(Gc + 1e-6);

        Ice = Ibe * beta - Ibc * beta;
        return 1e-6;
    }

    @Override
    public void addResidual(DMatrixRMaj residual) {
        residual.add(node1.getIndex(), 0, -Ice + Ibc);
        residual.add(beJunction.getNode1().getIndex(), 0, -Ibe - Ibc);
        residual.add(node2.getIndex(), 0, Ibe + Ice);
    }

    @Override
    public float current() {
        return (float) (-Ice + super.current()
                + beJunction.current() - Ibe
                - bcJunction.current() + Ibc);
    }

    @Override
    public float power() {
        return (float) (-Ice * potentialDifference()
                + (beJunction.current() - Ibe) * beJunction.potentialDifference()
                + (bcJunction.current() - Ibc) * bcJunction.potentialDifference());
    }
}
