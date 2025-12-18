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

import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.ICouplingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;

import java.util.HashMap;
import java.util.Map;

public class OptimizingElectricalNetwork extends ElectricalNetwork {
    private int optimizerCounter;

    private final Map<INode, Integer> optimizerScores = new HashMap<>();
    private boolean lockScores = false;

    public OptimizingElectricalNetwork(boolean addGMin, SolverType solver) {
        super(addGMin, solver);
    }

    @Override
    public void prepare(int multiTicks) {
        if(optimizerCounter++ >= 5) {
            optimizerCounter = 0;
            optimizerRoutine();
        }
        super.prepare(multiTicks);
    }

    @Override
    public void addNode(INode node) {
        super.addNode(node);
        if(node instanceof FloatingNode) {
            // Only floating nodes get scores.
            optimizerScores.put(node, 10);
        } else if(node instanceof ICouplingNode coupling) {
            for(var coupled : coupling.coupledNodes()) {
                unoptimizeNode(coupled);
                optimizerScores.remove(coupled);
            }
        }
    }

    @Override
    public void addWire(AbstractElectricWire wire) {
        super.addWire(wire);
        if(wire.getNode1() != null)
            unoptimizeNode(wire.getNode1());
        if(wire.getNode2() != null)
            unoptimizeNode(wire.getNode2());
    }

    @Override
    public void removeNode(INode node) {
        super.removeNode(node);
        optimizerScores.remove(node);
    }

    @Override
    public void makeLeaf(IElectricNode node, IElectricNode tracked) {
        super.makeLeaf(node, tracked);
        optimizerScores.remove(node);
    }

    @Override
    protected void jacobianAdd(int row, int column, double value) {
        super.jacobianAdd(row, column, value);
        if(lockScores)
            return;
        // Touched nodes get a penalty score since they are causing recalculations,
        // and should not go into the eliminated matrix.
        optimizerScores.computeIfPresent(nodes.get(row), ($, score) -> Math.min(score + 1, 40));
        optimizerScores.computeIfPresent(nodes.get(column), ($, score) -> Math.min(score + 1, 40));
    }

    @Override
    protected void residualAdd(int row, double value) {
        super.residualAdd(row, value);
        // Nodes with dynamic residuals cannot be optimized right now.
        var node = nodes.get(row);
        optimizerScores.remove(node);
        unoptimizeNode(node);
    }

    @Override
    protected void populateConductanceMatrix(boolean withEliminated) {
        lockScores = true;
        super.populateConductanceMatrix(withEliminated);
        lockScores = false;
    }

    protected boolean canOptimize(INode node) {
        return true;
    }

    private void optimizerRoutine() {
        for(var entry : optimizerScores.entrySet()) {
            var node = entry.getKey();
            if(!canOptimize(node)) {
                entry.setValue(20);
                unoptimizeNode(node);
                continue;
            }
            var score = entry.getValue();
            if(score >= 10) {
                unoptimizeNode(node);
            } else if(score <= 0) {
                optimizeNode(node);
            }
            if(score > 0) {
                entry.setValue(Math.max(score - 2, 0));
            }
        }
    }

    @Override
    public void merge(ElectricalNetwork other) {
        if(other == this)
            return;
        // This ensures proper (un)optimization
        other.nodes.forEach(node -> {
            if(node instanceof IElectricNode)
                addNode(node);
        });
        other.leafNodes.forEach((node, tracked) -> {
            node.setNetwork(this);
            leafNodes.put(node, tracked);
        });
        other.nodes.forEach(node -> {
            if(node instanceof ICouplingNode)
                addNode(node);
        });
        other.wires.forEach(this::addWire);
        // Make the other network empty.
        other.clear();
    }

    @Override
    public void clear() {
        super.clear();
        optimizerScores.clear();
    }
}
