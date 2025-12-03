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
package org.patryk3211.powergrid.electricity.sim.node;

import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IStaticResidual;

import java.util.List;

/**
 * Warning! Current source nodes cannot be directly connected to transformer couplings.
 */
public class CurrentSourceNode extends ElectricNode implements IStaticResidual {
    protected float current;

    public CurrentSourceNode() {

    }

    @Override
    public boolean isSource() {
        return true;
    }

    @Override
    public List<INode> affectedNodes() {
        return List.of(this);
    }

    public CurrentSourceNode(float current) {
        setCurrent(current);
    }

    @Override
    public float getVoltage() {
        return (float) getStateValue();
    }

    @Override
    public void setCurrent(float current) {
        this.current = current;
    }

    @Override
    public float getCurrent() {
        return current;
    }

    @Override
    public void addStaticResidual(IResidualAdder residual) {
        residual.add(getIndex(), current);
    }
}
