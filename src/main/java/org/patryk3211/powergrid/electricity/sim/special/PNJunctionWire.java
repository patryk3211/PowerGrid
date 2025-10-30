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

import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

public class PNJunctionWire extends DynamicConductanceWire {
    private double temperatureCelsius;
    private final double reverseSaturationCurrent;
    private final double seriesResistance;
    private final double idealityFactor;

    public PNJunctionWire(double reverseSaturationCurrent, double seriesResistance, double temperatureCelsius, double idealityFactor, IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
        this.reverseSaturationCurrent = reverseSaturationCurrent;
        this.seriesResistance = seriesResistance;
        this.temperatureCelsius = temperatureCelsius;
        this.idealityFactor = idealityFactor;
    }

    private static double LambertW(double z)
    {
        double PRECISION = 1E-12;
        double S = 0.0;
        for (int n=1; n <= 100; n++)
        {
            double Se = S * StrictMath.pow(StrictMath.E, S);
            double S1e = (S+1) *
                    StrictMath.pow(StrictMath.E, S);
            if (PRECISION > StrictMath.abs((z-Se)/S1e))
            {
                return S;
            }
            S -=
                    (Se-z) / (S1e - (S+2) * (Se-z) / (2*S+2));
        }
        return S;
    }

    public void setTemperatureCelsius(double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public double calculateConductance() {
        // TODO: reverse breakdown
        double k = 1.380649e-23; // Boltzmann constant in J/K
        double q = 1.602176634e-19; // Elementary charge in C
        double V_T = (k * (temperatureCelsius + 273.15)) / q; // Thermal voltage in V
        double n = idealityFactor;
        double V = potentialDifference();
        double I_s1 = reverseSaturationCurrent;
        double E_g = 1.12; // Silicon bandgap energy in eV
        double T_1 = 22 + 273.15; // Reference temperature in K
        double T_2 = temperatureCelsius + 273.15; // Actual temperature in K
        double T_2_div_T_1 = T_2 / T_1;
        double I_s2 = I_s1 * Math.pow(T_2_div_T_1, 3/n) * Math.exp(- (q * E_g / k / T_2 / n) * (1 - T_2_div_T_1));
        double R_s = seriesResistance;
        // Banwell and Jayakumar (2000)
        double IsRs = I_s2 * R_s;
        double W_x = IsRs / n / V_T * Math.exp((IsRs + V) / V_T / n);
        double WTerm = LambertW(W_x);
        double result = 0;
        if (Math.abs(V) < 1e-9) {
            // dI/dV at V=0 to avoid division by zero
            result = WTerm / (R_s * (1 + WTerm));
        }
        else {
            result = (n * V_T / R_s * WTerm - I_s2) / V;
        }
        return Math.max(result, ElectricalNetwork.G_MIN);
    }
}