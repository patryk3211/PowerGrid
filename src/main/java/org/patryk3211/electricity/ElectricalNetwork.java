/**
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
 **/
package org.patryk3211.electricity;

import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ElectricalNetwork {
    private static final double PRECISION = 1e-6;

    private Set<ElectricWire> wires = new HashSet<>();
    private final ArrayList<IElectricNode> nodes = new ArrayList<>();

    private ISolver solver;
    private DMatrixRMaj conductanceMatrix;
    private DMatrixRMaj currentMatrix;

    public ElectricalNetwork() {
        solver = new BiCGSTABSolver(PRECISION);

        var N1 = new VoltageSourceNode();
        var N2 = new FloatingNode();
        var N3 = new FloatingNode();
        var N4 = new VoltageSourceNode();

        N1.setVoltage(5);
        N4.setVoltage(2);

        addNode(N1);
        addNode(N2);
        addNode(N3);
        addNode(N4);

        wires.add(new ElectricWire(2, N1, N2));
        wires.add(new ElectricWire(4, N2, null));
        wires.add(new ElectricWire(5, N2, N3));
        wires.add(new ElectricWire(10, N3, null));
        wires.add(new ElectricWire(10, N3, null));
        wires.add(new ElectricWire(10, N1, N3));
        wires.add(new ElectricWire(5, N4, N2));
    }

    public void addNode(IElectricNode node) {
        node.assignIndex(nodes.size());
        nodes.add(node);
    }

    public void calculate() {
        var size = nodes.size();
        if(conductanceMatrix == null || conductanceMatrix.getNumRows() != size) {
            conductanceMatrix = new DMatrixRMaj(size, size);
            currentMatrix = new DMatrixRMaj(size, 1);
        }
        solver.setStateSize(size);

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

        currentMatrix.zero();
        for(var node : nodes) {
            if(node instanceof VoltageSourceNode source) {
                var U = source.getVoltage();
                var index = node.getIndex();

                for(int i = 0; i < size; ++i) {
                    currentMatrix.add(i, 0, -U * conductanceMatrix.get(i, index));
                    conductanceMatrix.set(i, index, 0);
                }
                conductanceMatrix.set(index, index, -1);
            }
        }

//        for(int i = 0; i < size; ++i) {
//            for(int j = 0; j < size; ++j) {
//                System.out.printf("%7.3f ", conductanceMatrix.get(i, j));
//            }
//            System.out.printf("   %7.3f\n", currentMatrix.get(i, 0));
//        }

        var result = solver.solve(conductanceMatrix, currentMatrix);

        for(var node : nodes) {
            node.receiveResult((float) result.get(node.getIndex(), 0));
        }
    }
}
