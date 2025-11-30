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

import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DummyElectricalNetwork extends GraphedElectricalNetwork {
    public DummyElectricalNetwork(NetworkGraph graph) {
        super(graph, false);
    }

    @Override
    public void updateConductance(AbstractElectricWire wire, double change) { }
    @Override
    public void alterConductanceMatrix(int row, int column, double change) { }
    @Override
    protected void jacobianAdd(int row, int column, double value) { }
    @Override
    protected void rhsAdd(int row, double value) { }
    @Override
    protected void residualAdd(int row, double value) { }
    @Override
    public void optimizeNode(@NotNull INode node) { }
    @Override
    public void unoptimizeNode(@NotNull INode node) { }
    @Override
    public void makeLeaf(IElectricNode node, IElectricNode tracked) { }
    @Override
    public void removeLeaf(IElectricNode node) { }
    @Override
    protected void checkConnectivity(IElectricNode node, Set<IElectricNode> outerChecked) { }

    @Override
    public List<INode> findProblematicNodes(DMatrixRMaj residual, double threshold) {
        return List.of();
    }
    @Override
    public Collection<AbstractElectricWire> findProblematicWires(DMatrixRMaj residual, double threshold) {
        return List.of();
    }

    @Override
    protected void prepareMatrices(int multiTicks) {
        // Client is not simulated so don't bother with allocating any other matrices.
        // The network is basically just there to hold the state vector and make all other code,
        // that expects a network to work.
        var shouldReallocate = StateVector == null || dirty || StateVector.getNumRows() != nodes.size();
        if(shouldReallocate) {
            var count = nodes.size();
            var NewState = new DMatrixRMaj(count, 1);
            // Use previous state matrix to accelerate warm up
            if(StateVector != null) {
                for (int i = 0; i < count; ++i) {
                    NewState.set(i, 0, getValue(nodes.get(i)));
                }
            }

            StateVector = NewState;
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
}
