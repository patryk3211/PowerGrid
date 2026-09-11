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

public class ThyratronWire extends ColdCathodeRegulatorTubeWire {
    private static final float EMISSION_FIRE_THRESHOLD = 0.05f;

    private final IElectricNode grid;
    private final float controlRatio;
    private float emission = 1f;

    public ThyratronWire(float controlRatio, float breakdownVoltage, float holdingVoltage, float holdingCurrent, float dischargeConductance,
                         IElectricNode cathode, IElectricNode anode, IElectricNode grid) {
        super(breakdownVoltage, holdingVoltage, holdingCurrent, dischargeConductance, anode, cathode);
        this.grid = grid;
        this.controlRatio = controlRatio;
    }

    public void setEmission(float emission) {
        this.emission = emission;
    }

    public float getEmission() {
        return emission;
    }

    public double gridVoltage() {
        return grid.getVoltage() - node2.getVoltage();
    }

    public double strikeVoltage() {
        return strikeVoltage(gridVoltage());
    }

    public double strikeVoltage(double vg) {
        return Math.max(holdingVoltage, breakdownVoltage - controlRatio * vg);
    }

    @Override
    public void preSolve() {
        if (!isConverged())
            return;

        double V = potentialDifference();
        double I = current();
        if (litTicks <= 0) {
            if (emission > EMISSION_FIRE_THRESHOLD && V > strikeVoltage()) {
                updateConductance(dischargeConductance);
                litTicks = 2;
            }
        } else if (I < holdingCurrent || V <= 0) {
            if (--litTicks <= 0) {
                updateConductance(OFF_CONDUCTANCE);
                litTicks = 0;
            }
        } else {
            litTicks = 2;
        }
    }

    @Override
    public String toString() {
        return String.format("Thyratron(μ=%g Vb=%g Vh=%g Ih=%g G=%g)",
                controlRatio, breakdownVoltage, holdingVoltage, holdingCurrent, dischargeConductance);
    }
}
