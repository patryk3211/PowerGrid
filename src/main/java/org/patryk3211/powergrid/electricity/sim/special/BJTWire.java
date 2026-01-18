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

    private final ConductanceWire collectorBase;
    private final CrossWire collectorEmitter;
    private final CrossWire emitterCollector;

    private final double forwardGain, reverseGain;
    private final double saturationCurrent;
    private final double seriesResistance;

    private final double OmegaLog;

    private double prevCollector;
    private double prevEmitter;
    private double prevBase;

    private double Ic, Ib, Ie;

    public BJTWire(IElectricNode collector, IElectricNode base, IElectricNode emitter, double Is, double fBeta, double Rs) {
        super(base, emitter);
        this.collector = collector;
        // alpha = beta / (beta + 1)
        forwardGain = fBeta / (fBeta + 1);
        reverseGain = 1 / 2f;
        saturationCurrent = Is;
        seriesResistance = Rs;

        OmegaLog = Math.log(saturationCurrent * seriesResistance / V_T);

        collectorBase = addDynamicWire(collector, base);
        collectorEmitter = addInternalWire(new CrossWire(base, collector, emitter));
        emitterCollector = addInternalWire(new CrossWire(base, emitter, collector));
    }

    @Override
    public void startIteration() {
        // Get smooth potentials
        double Vc = collector.getVoltage();
        double Vb = node1.getVoltage();
        double Ve = node2.getVoltage();
        var dVc = Vc - prevCollector;
        var dVb = Vb - prevBase;
        var dVe = Ve - prevEmitter;
        Vc = prevCollector + softDelta(Math.abs(dVc), 0.1) * Math.signum(dVc);
        Vb = prevBase + softDelta(Math.abs(dVb), 0.1) * Math.signum(dVb);
        Ve = prevEmitter + softDelta(Math.abs(dVe), 0.1) * Math.signum(dVe);
        prevEmitter = Ve;
        prevCollector = Vc;
        prevBase = Vb;

        double Vbe = Vb - Ve, Vbc = Vb - Vc;

        double IsRs = saturationCurrent * seriesResistance;
        double WbeTerm = WrightOmega(OmegaLog + (IsRs + Vbe) / V_T);
        double WbcTerm = WrightOmega(OmegaLog + (IsRs + Vbc) / V_T);
        double Ebe = V_T * WbeTerm / seriesResistance - saturationCurrent;
        double Ebc = V_T * WbcTerm / seriesResistance - saturationCurrent;

        double Ie = Ebc * reverseGain - Ebe;
        double Ic = Ebe * forwardGain - Ebc;
        double Ib = -Ie - Ic;

        double Gee = Math.max(WbeTerm / (seriesResistance * (1 + WbeTerm)), G_MIN);
        double Gcc = Math.max(WbcTerm / (seriesResistance * (1 + WbcTerm)), G_MIN);
        double Gce = -Gee * forwardGain;
        double Gec = -Gcc * reverseGain;

        double G_add = 1e-5;

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
    }
}
