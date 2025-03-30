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
package org.patryk3211.electricity;

import org.ejml.data.DMatrixRMaj;
import org.patryk3211.electricity.node.ICouplingNode;
import org.patryk3211.electricity.node.IElectricNode;
import org.patryk3211.electricity.node.INode;
import org.patryk3211.electricity.node.VoltageSourceNode;
import org.patryk3211.electricity.solver.BiCGSTABSolver;
import org.patryk3211.electricity.solver.ISolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ElectricalNetwork {
    private static final double PRECISION = 1e-6;

    private final Set<ElectricWire> wires = new HashSet<>();
    private final Set<ICouplingNode> couplings = new HashSet<>();
    private final ArrayList<INode> nodes = new ArrayList<>();

    private final ISolver solver;
    private DMatrixRMaj conductanceMatrix;
    private DMatrixRMaj currentMatrix;

    private boolean dirty;

    public ElectricalNetwork() {
        solver = new BiCGSTABSolver(PRECISION);
        dirty = true;
    }

    // Make sure all variables are completely rebuilt and repopulated.
    public void setDirty() {
        this.dirty = true;
    }

    public void addNode(IElectricNode node) {
        node.assignIndex(nodes.size());
        nodes.add(node);
        setDirty();
    }

    public void addNodes(IElectricNode... nodes) {
        for(var node : nodes)
            addNode(node);
    }

    public void removeNode(INode node) {
        if(nodes.get(node.getIndex()) != node)
            // This node is not actually in this network.
            return;

        if(nodes.size() > 1) {
            // Move last node into the place of removed node to prevent holes in the array.
            var last = nodes.get(nodes.size() - 1);
            nodes.set(node.getIndex(), last);
            nodes.remove(nodes.size() - 1);
            last.assignIndex(node.getIndex());
        } else {
            // This is the only node so it's ok to just remove it.
            nodes.remove(node);
        }

        if(node instanceof ICouplingNode)
            couplings.remove(node);

        setDirty();
    }

    public void removeNode(int index) {
        if(nodes.size() <= index)
            return;

        removeNode(nodes.get(index));
    }

    public void addWire(ElectricWire wire) {
        if((wire.node1 != null && !nodes.contains(wire.node1)) || (wire.node2 != null && !nodes.contains(wire.node2)))
            // If node of a wire is not null it must be in the network's node set.
            throw new IllegalArgumentException("Both nodes of a wire must be part of the network");
        wire.setNetwork(this);
        wires.add(wire);

        updateResistance(wire, 0);
    }

    public void removeWire(ElectricWire wire) {
        if(!wires.contains(wire))
            return;
        wires.remove(wire);

//        double change = -wire.conductance();
//        if(wire.node1 != null && wire.node2 != null) {
//            var index1 = wire.node1.getIndex();
//            var index2 = wire.node2.getIndex();
//            conductanceMatrix.add(index1, index1, change);
//            conductanceMatrix.add(index2, index2, change);
//            conductanceMatrix.add(index1, index2, -change);
//            conductanceMatrix.add(index2, index1, -change);
//        } else {
//            var index = wire.node1 != null ? wire.node1.getIndex() : wire.node2.getIndex();
//            conductanceMatrix.add(index, index, change);
//        }
    }

    public void updateResistance(ElectricWire wire, double oldResistance) {
//        if(conductanceMatrix == null || dirty)
//            return;
//
//        double change;
//        if(oldResistance != 0)
//            change = wire.conductance() - 1 / oldResistance;
//        else
//            change = wire.conductance();
//
//        if(wire.node1 != null && wire.node2 != null) {
//            var index1 = wire.node1.getIndex();
//            var index2 = wire.node2.getIndex();
//            conductanceMatrix.add(index1, index1, change);
//            conductanceMatrix.add(index2, index2, change);
//            conductanceMatrix.add(index1, index2, -change);
//            conductanceMatrix.add(index2, index1, -change);
//        } else {
//            var index = wire.node1 != null ? wire.node1.getIndex() : wire.node2.getIndex();
//            conductanceMatrix.add(index, index, change);
//        }
    }

    public void addNode(ICouplingNode coupling) {
        coupling.assignIndex(nodes.size());
        coupling.setNetwork(this);
        couplings.add(coupling);
        nodes.add(coupling);
        setDirty();
    }

    private void populateConductanceMatrix() {
        conductanceMatrix.zero();
        for(var wire : wires) {
            var G = wire.conductance();
            if(wire.node1 != null && wire.node2 != null) {
                var index1 = wire.node1.getIndex();
                var index2 = wire.node2.getIndex();
                conductanceMatrix.add(index1, index1, G);
                conductanceMatrix.add(index2, index2, G);
                conductanceMatrix.add(index1, index2, -G);
                conductanceMatrix.add(index2, index1, -G);
            } else {
                var index = wire.node1 != null ? wire.node1.getIndex() : wire.node2.getIndex();
                conductanceMatrix.add(index, index, G);
            }
        }

        for(var node : couplings) {
            node.couple(conductanceMatrix);
        }
    }

    private void populateCurrentMatrix() {
        currentMatrix.zero();
        final var mat = conductanceMatrix;
        for(var node : nodes) {
            if(node instanceof VoltageSourceNode source) {
                var U = source.getVoltage();
                var index = node.getIndex();

                for(int i = 0; i < nodes.size(); ++i) {
                    currentMatrix.add(i, 0, -U * mat.get(i, index));
                    mat.set(i, index, 0);
                }
                mat.set(index, index, -1);
            }
        }
    }

    public void calculate() {
        var nodeCount = nodes.size();
        if(conductanceMatrix == null || dirty || conductanceMatrix.getNumRows() != nodeCount) {
            conductanceMatrix = new DMatrixRMaj(nodeCount, nodeCount);
            currentMatrix = new DMatrixRMaj(nodeCount, 1);
            solver.setStateSize(nodeCount);
            dirty = false;

            // Conductance and coupling matrices need to be fully rebuild only after a state size change,
            // individual resistance and coupling value changes are handled by `updateResistance()` and `updateCoupling()` respectively.
        }

        populateConductanceMatrix();
        populateCurrentMatrix();

        var result = solver.solve(conductanceMatrix, currentMatrix);
        for(var node : nodes) {
            if(node instanceof IElectricNode enode)
                enode.receiveResult((float) result.get(node.getIndex(), 0));
        }
    }
}
