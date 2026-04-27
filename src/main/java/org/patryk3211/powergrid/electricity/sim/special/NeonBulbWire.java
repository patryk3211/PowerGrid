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
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IStaticResidual;

public class NeonBulbWire extends AbstractElectricWire implements IOuterHook, IStaticResidual {
    public static final double OFF_CONDUCTANCE = 1e-6;

    protected final float breakdownVoltage;
    protected final float holdingVoltage;
    protected final float holdingCurrent;
    protected final float dischargeConductance;
    protected double conductance;

    protected int litTicks = 0;
    protected double I;

    public NeonBulbWire(float breakdownVoltage, float holdingVoltage, float holdingCurrent, float dischargeConductance, IElectricNode anode, IElectricNode cathode) {
        super(anode, cathode);
        this.breakdownVoltage = breakdownVoltage;
        this.holdingVoltage = holdingVoltage;
        this.holdingCurrent = holdingCurrent;
        this.dischargeConductance = dischargeConductance;
        this.conductance = OFF_CONDUCTANCE;
    }

    public void setLit(boolean lit) {
        if(isLit() != lit) {
            this.litTicks = lit ? 2 : 0;
            updateConductance(lit ? dischargeConductance : OFF_CONDUCTANCE);
            if(network != null)
                network.warmUp(1);
        }
    }

    public boolean isLit() {
        return litTicks > 0;
    }

    @Override
    public double conductance() {
        return conductance;
    }

    public void updateConductance(double conductance) {
        if(network != null)
            network.updateConductance(this, conductance - this.conductance);
        this.conductance = conductance;
    }

    @Override
    public void addStaticResidual(IResidualAdder residual) {
        if(isLit()) {
            I = holdingVoltage * dischargeConductance * Math.signum(potentialDifference());
            residual.add(node1.getIndex(), I);
            residual.add(node2.getIndex(), -I);
        } else {
            I = 0;
        }
    }

    @Override
    public double current() {
        return super.current() - I;
    }

    @Override
    public void preSolve() {
        if(!isConverged())
            return;
        // Update discharge state
        double V = Math.abs(potentialDifference()), I = Math.abs(current());
        if(litTicks <= 0 && V > breakdownVoltage) {
            updateConductance(dischargeConductance);
            litTicks = 2;
        } else if(litTicks > 0 && (I < holdingCurrent || V < holdingVoltage)) {
            if(--litTicks <= 0) {
                updateConductance(OFF_CONDUCTANCE);
                litTicks = 0;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("NeonBulb(Vb=%g Vh=%g Ih=%g G=%g)", breakdownVoltage, holdingVoltage, holdingCurrent, dischargeConductance);
    }
}
