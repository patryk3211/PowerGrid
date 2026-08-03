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
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

import static org.patryk3211.powergrid.electricity.sim.special.PNJunctionWire.WrightOmega;

public class PNJunctionWireSolar extends AbstractElectricWire implements ISolverHook {
    private double temperatureCelsius;
    private final double reverseSaturationCurrent;
    private final double seriesResistance;
    private double idealityFactor;
    private final double breakdownVoltage;
    private final double breakdownSaturationCurrent;
    private final double singleIdealityFactor;

    private double G = ElectricalNetwork.G_MIN;
    private double Ieq = 0;
    private double prevV;

    public int iterationLimit = -1;

    public PNJunctionWireSolar(double reverseSaturationCurrent, double seriesResistance, double temperatureCelsius, double idealityFactor, double junctionIdealityFactor, IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
        this.reverseSaturationCurrent = reverseSaturationCurrent;
        this.seriesResistance = seriesResistance;
        this.temperatureCelsius = temperatureCelsius;
        this.idealityFactor = idealityFactor;
        this.singleIdealityFactor = junctionIdealityFactor;
        this.breakdownVoltage = 0;
        this.breakdownSaturationCurrent = 0;
    }

    public double pnLim(double V1, double V0, double Vcrit, double V_T) {
        if(V0 < 0 && V1 > Vcrit)
            return 0;
        if(V0 >= 0 && V0 < Vcrit && V1 > Vcrit)
            return Vcrit;
        var dV = V1 - V0;
        if(V1 > Vcrit && dV > V_T * 2)
            return V0 + idealityFactor * V_T * Math.log1p(dV / (idealityFactor * V_T));
        return V1;
    }

    public void setTemperatureCelsius(double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    @Override
    public double current() {
        return this.Ieq + this.G * potentialDifference();
    }

    @Override
    public double conductance() {
        return G;
    }

    @Override
    public void startIteration(int iteration) {
        if(iterationLimit > 0 && iteration > iterationLimit)
            return;
        double k = 1.380649e-23; // Boltzmann constant in J/K
        double q = 1.602176634e-19; // Elementary charge in C
        double V_T = (k * (temperatureCelsius + 273.15)) / q; // Thermal voltage in V
        double n = idealityFactor;
        double V = potentialDifference();
        double Vcrit = n * V_T * Math.log((n * V_T) / (reverseSaturationCurrent * Math.sqrt(2)));
        prevV = V = pnLim(V, prevV, Vcrit, V_T);
        double I_s1 = reverseSaturationCurrent;
        double E_g = 1.12; // Silicon bandgap energy in eV
        double T_1 = 22 + 273.15; // Reference temperature in K
        double T_2 = temperatureCelsius + 273.15; // Actual temperature in K
        double T_2_div_T_1 = T_2 / T_1;
        double I_s2 = I_s1 * Math.pow(T_2_div_T_1, 3 / singleIdealityFactor)
                * Math.exp(- (q * E_g / k / T_2 / singleIdealityFactor) * (1 - T_2_div_T_1));
        double R_s = seriesResistance;
        // Banwell and Jayakumar (2000)
        double IsRs = I_s2 * R_s;
        double Omega_arg = Math.log(IsRs / n / V_T) + (IsRs + V) / (n * V_T);
        double WTerm = WrightOmega(Omega_arg);
        double G = Math.max(WTerm / (R_s * (1 + WTerm)), ElectricalNetwork.G_MIN);

        double I = V_T * n * WTerm / R_s - I_s2;
        if(breakdownVoltage > 0) {
            // Reverse breakdown using a shifted diode current curve
            double V_over = -breakdownVoltage - V; //so only when in reverse bias
            double B_IsRs = breakdownSaturationCurrent * R_s;
            double B_Omega_arg = Math.log(B_IsRs / n / V_T) + (B_IsRs + V_over) / (n * V_T);
            double B_WTerm = WrightOmega(B_Omega_arg);
            G += Math.max(B_WTerm / (R_s * (1 + B_WTerm)), ElectricalNetwork.G_MIN);
            I -= V_T * n * B_WTerm / R_s - breakdownSaturationCurrent;
        }

        // Adding a resistor across the diode helps with convergence in certain cases.
        double G_add = 1e-6;
        if(iteration > 100) {
            G_add = 1e-4;
        }
        G += G_add;
        network.updateConductance(this, G - this.G);
        this.G = G;

        // Compute Ieq
        this.Ieq = I - G * V;
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        residual.add(node1.getIndex(), Ieq);
        residual.add(node2.getIndex(), -Ieq);
    }

    public void setIdealityFactor(double idealityFactor) {
        this.idealityFactor = idealityFactor;
    }

    @Override
    public String toString() {
        return String.format("PNJunction(Is=%g)#%d", reverseSaturationCurrent, System.identityHashCode(this));
    }
}