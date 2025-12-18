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

public class PNJunctionWire extends AbstractElectricWire implements ISolverHook {
    private double temperatureCelsius;
    private final double reverseSaturationCurrent;
    private final double seriesResistance;
    private final double idealityFactor;
    private double G = ElectricalNetwork.G_MIN;
    private double Ieq = 0;
    private double prevV;

    public PNJunctionWire(double reverseSaturationCurrent, double seriesResistance, double temperatureCelsius, double idealityFactor, IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
        this.reverseSaturationCurrent = reverseSaturationCurrent;
        this.seriesResistance = seriesResistance;
        this.temperatureCelsius = temperatureCelsius;
        this.idealityFactor = idealityFactor;
    }

    private static double WrightOmega(double z)
    {
        // D'Angelo, Gabrielli and Turchet (2019) approximation
        double x1 = -3.341459552768620;
        double x2 = 8;
        double alpha = -1.314293149877800e-3;
        double beta = 4.775931364975583e-2;
        double gamma = 3.631952663804445e-1;
        double zeta = 6.313183464296682e-1;
        if (z <= x1)
        {
            return 0;
        }
        else if (z < x2)
        {
            return alpha * z * z * z + beta * z * z + gamma * z + zeta;
        }
        else
        {
            return z - Math.log(z);
        }
    }

    public void setTemperatureCelsius(double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    @Override
    public float current() {
        return (float) (this.Ieq + this.G * potentialDifference());
    }

    @Override
    public double conductance() {
        return G;
    }

    @Override
    public void startIteration() {
        // TODO: reverse breakdown
        double k = 1.380649e-23; // Boltzmann constant in J/K
        double q = 1.602176634e-19; // Elementary charge in C
        double V_T = (k * (temperatureCelsius + 273.15)) / q; // Thermal voltage in V
        double n = idealityFactor;
        double V = potentialDifference();
        var dV = V - prevV;
        V = prevV + softDelta(Math.abs(dV), 0.8) * Math.signum(dV);
        prevV = V;
        double I_s1 = reverseSaturationCurrent;
        double E_g = 1.12; // Silicon bandgap energy in eV
        double T_1 = 22 + 273.15; // Reference temperature in K
        double T_2 = temperatureCelsius + 273.15; // Actual temperature in K
        double T_2_div_T_1 = T_2 / T_1;
        double I_s2 = I_s1 * Math.pow(T_2_div_T_1, 3/n) * Math.exp(- (q * E_g / k / T_2 / n) * (1 - T_2_div_T_1));
        double R_s = seriesResistance;
        // Banwell and Jayakumar (2000)
        double IsRs = I_s2 * R_s;
        double Omega_arg = Math.log(IsRs / n / V_T) + (IsRs + V) / (n * V_T);
        double WTerm = WrightOmega(Omega_arg);
        double G = Math.max(WTerm / (R_s * (1 + WTerm)), ElectricalNetwork.G_MIN);
        // Adding a resistor across the diode helps with convergence in certain cases.
        G += 1e-6; // 1 MOhm
        network.updateConductance(this, G - this.G);
        this.G = G;
        double I = V_T * n * WTerm / R_s - I_s2;
        this.Ieq = I - (G - 1e-6) * V;
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        residual.add(node1.getIndex(), Ieq);
        residual.add(node2.getIndex(), -Ieq);
    }
}