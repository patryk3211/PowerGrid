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
package org.patryk3211.powergrid.electricity.sim;

import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.sim.solver.IMatrixAccess;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DummyElectricalNetwork extends GraphedElectricalNetwork {
    private double[] nodeValues;
    private int warmUpTicks;
    private boolean converged;

    public DummyElectricalNetwork(NetworkGraph graph) {
        super(graph, false, null);
    }

    @Override
    public void cleanup() { }
    @Override
    public void setPrecision(double absoluteCriterion, double relativeCriterion, double minimumPrecision) { }

    @Override
    public void warmUp(int ticks) {
        if(ticks == -1 || warmUpTicks == -1) {
            warmUpTicks = -1;
            return;
        }
        converged = false;
        if(warmUpTicks < ticks)
            warmUpTicks = ticks;
    }

    @Override
    public boolean isConverged() {
        return converged;
    }

    @Override
    public double getValue(INode node) {
        if(nodeValues == null)
            return 0;
        var index = node.getIndex();
        if(index < 0 || index >= nodeValues.length)
            return 0 ;
        return nodeValues[node.getIndex()];
    }

    @Override
    public void setValue(INode node, double value) {
        if(nodeValues == null)
            return;
        var index = node.getIndex();
        if(index < 0 || index >= nodeValues.length)
            return;
        nodeValues[node.getIndex()] = value;
    }

    @Override
    public void updateConductance(AbstractElectricWire wire, double change) { }
    @Override
    public void alterConductanceMatrix(int row, int column, double change) { }
    @Override
    public void makeLeaf(IElectricNode node, IElectricNode tracked) { }
    @Override
    public void removeLeaf(IElectricNode node) { }
    @Override
    protected void checkConnectivity(IElectricNode node, Set<IElectricNode> outerChecked) { }

    @Override
    public List<INode> findProblematicNodes(IMatrixAccess residual, double threshold) {
        return List.of();
    }
    @Override
    public Collection<AbstractElectricWire> findProblematicWires(IMatrixAccess residual, double threshold) {
        return List.of();
    }

    @Override
    protected void prepareMatrices(int multiTicks) {
        // Client is not simulated so don't bother with allocating any other matrices.
        // The network is basically just there to hold the state vector and make all other code,
        // that expects a network to work.
        var shouldReallocate = dirty || nodeValues.length != nodes.size();
        if(shouldReallocate) {
            var count = nodes.size();
            var NewState = new double[count];
            // Use previous state matrix to accelerate warm up
            if(nodeValues != null) {
                for (int i = 0; i < count; ++i) {
                    NewState[i] = getValue(nodes.get(i));
                }
            }

            nodeValues = NewState;
            dirty = false;
        }
    }

    @Override
    public void calculate(int multiTicks) {
        prepareMatrices(multiTicks);
        if(warmUpTicks > 0) {
            converged = false;
            warmUpTicks = 0;
        } else {
            converged = true;
        }
    }

    @Override
    public void prepare(int multiTicks) {
        prepareMatrices(multiTicks);
    }

    @Override
    public void singleTick() {
        if(warmUpTicks > 0) {
            converged = false;
            warmUpTicks = 0;
        } else {
            converged = true;
        }
    }
}
