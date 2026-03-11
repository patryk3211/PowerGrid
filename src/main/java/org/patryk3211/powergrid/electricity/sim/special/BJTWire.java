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
package org.patryk3211.powergrid.electricity.sim.special;

import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IAdmittanceAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

import java.util.Collection;
import java.util.List;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.G_MIN;
import static org.patryk3211.powergrid.electricity.sim.special.PNJunctionWire.WrightOmega;

public class BJTWire extends CompoundWire implements ISolverHook {
    private static final double V_T = 0.025;

    private final IElectricNode collector;

    protected final ConductanceWire collectorBase;
    private final CrossWire collectorEmitter;
    private final CrossWire emitterCollector;

    private final double beta;
    private final double forwardGain, reverseGain;
    private final double saturationCurrent;
    private final double emitterResistance, collectorResistance;
    private final int pnp;
    private final double Vcrit;

    private final double OmegaLogE, OmegaLogC;

    private double prevCollector;
    private double prevEmitter;

    private double Ic, Ib, Ie;

    private double power;

    public BJTWire(IElectricNode collector, IElectricNode base, IElectricNode emitter, double Is, double fBeta, double Rs, boolean pnp) {
        super(base, emitter);
        this.collector = collector;
        this.beta = fBeta;
        // alpha = beta / (beta + 1)
        forwardGain = fBeta / (fBeta + 1);
        reverseGain = Math.max(0.5, fBeta * 0.1 / (fBeta * 0.1 + 1));
        saturationCurrent = Is;
        if(pnp) {
            emitterResistance = Rs * 10;
            collectorResistance = Rs;
        } else {
            emitterResistance = Rs;
            collectorResistance = Rs * 10;
        }
        this.pnp = pnp ? -1 : 1;

        OmegaLogE = Math.log(saturationCurrent * emitterResistance / V_T);
        OmegaLogC = Math.log(saturationCurrent * collectorResistance / V_T);

        collectorBase = addDynamicWire(collector, base);
        collectorEmitter = addInternalWire(new CrossWire(base, collector, emitter));
        emitterCollector = addInternalWire(new CrossWire(base, emitter, collector));
        Vcrit = V_T * Math.log(V_T / (Is * Math.sqrt(2)));
    }

    public double pnLim(double V1, double V0, double Vcrit) {
        if(V0 < Vcrit && V1 < Vcrit)
            return V1;
        var dV = V1 - V0;
        return V0 + dV * network.bjtSmoothAlpha;
    }

    @Override
    public void startIteration(int iteration) {
        // Get smooth potentials
        double Vc = collector.getVoltage();
        double Vb = node1.getVoltage();
        double Ve = node2.getVoltage();
        double Vbe = Vb - Ve, Vbc = Vb - Vc;
        Vbc = prevCollector = pnp * pnLim(pnp * Vbc, pnp * prevCollector, Vcrit);
        Vbe = prevEmitter = pnp * pnLim(pnp * Vbe, pnp * prevEmitter, Vcrit);

        double WbeTerm = WrightOmega(OmegaLogE + (saturationCurrent * emitterResistance + pnp * Vbe) / V_T);
        double WbcTerm = WrightOmega(OmegaLogC + (saturationCurrent * collectorResistance + pnp * Vbc) / V_T);
        double Ebe = V_T * WbeTerm / emitterResistance - saturationCurrent;
        double Ebc = V_T * WbcTerm / collectorResistance - saturationCurrent;

        double Ie = pnp * (Ebc * reverseGain - Ebe);
        double Ic = pnp * (Ebe * forwardGain - Ebc);
        double Ib = -Ie - Ic;

        double Gee = Math.max(WbeTerm / (emitterResistance * (1 + WbeTerm)), G_MIN);
        double Gcc = Math.max(WbcTerm / (collectorResistance * (1 + WbcTerm)), G_MIN);
        double Gce = -Gee * forwardGain;
        double Gec = -Gcc * reverseGain;

        double G_add = 1e-6;
        if(iteration > 100) {
            G_add = Math.min((iteration - 100) * 1e-3, 0.01);
        }

        // Base - Emitter, simple wire
        setConductance(Gee + G_add);
        // Base - Collector, simple wire
        collectorBase.setConductance(Gcc + G_add);
        // Collector - Emitter, VCCS
        collectorEmitter.setConductance(Gce);
        // Emitter - Collector, VCCS
        emitterCollector.setConductance(Gec);

        this.Ib = Ib - (G_add + Gcc + Gec) * Vbc - (G_add + Gee + Gce) * Vbe;
        this.Ic = Ic + (Gcc + G_add) * Vbc + Gce * Vbe;
        this.Ie = Ie + (Gee + G_add) * Vbe + Gec * Vbc;

        power = 0;
        if(Vbe > 0) {
            power += Vbe * Ib;
        }
        if(Vbc > 0) {
            power += Vbc * Ib;
        }
        if(Ic > 0) {
            power += (Vc - Ve) * Ic;
        }
        if(Ie > 0) {
            power += (Ve - Vc) * Ie;
        }
    }

    @Override
    public double power() {
        return power;
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        residual.add(collector.getIndex(), Ic);
        residual.add(node1.getIndex(), Ib);
        residual.add(node2.getIndex(), Ie);
    }

    @Override
    public Collection<IElectricNode> coupledNodes() {
        return List.of(collector, node1, node2);
    }

    @Override
    public String toString() {
        return String.format("%s-BJT(Is=%g, beta=%g)", pnp == -1 ? "P" : "N", saturationCurrent, beta);
    }

    private static class CrossWire extends ConductanceWire {
        private final IElectricNode base;

        public CrossWire(IElectricNode base, IElectricNode node1, IElectricNode node2) {
            super(node1, node2);
            this.base = base;
        }

        @Override
        public void stamp(IAdmittanceAdder admittance, double change) {
            admittance.add(base.getIndex(), base.getIndex(), change);
            admittance.add(base.getIndex(), node2.getIndex(), -change);
            admittance.add(node1.getIndex(), base.getIndex(), -change);
            admittance.add(node1.getIndex(), node2.getIndex(), change);
        }

        @Override
        public Collection<IElectricNode> coupledNodes() {
            return List.of(base, node1, node2);
        }

        @Override
        public String toString() {
            return String.format("BJT$CrossWire(G=%g)", conductance());
        }
    }
}
