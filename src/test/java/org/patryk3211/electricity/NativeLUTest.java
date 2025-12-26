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
import org.ejml.dense.row.decomposition.lu.LUDecompositionBase_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

import java.util.Arrays;

public class NativeLUTest {
    public static void main(String[] args) {
        var RefA = new DMatrixRMaj(3, 3);
        RefA.set(0, 0, 2);
        RefA.set(1, 1, 3);
        RefA.set(2, 2, 3);
        RefA.set(0, 1, -1);
        RefA.set(1, 0, -1);
        RefA.set(1, 2, -2);
        RefA.set(2, 1, -2);

        var b = new DMatrixRMaj(3, 1);
        b.set(0, 0, 4);
        b.set(1, 0, 2);
        b.set(2, 0, 1);

        var RefSolver = LinearSolverFactory_DDRM.lu(3);
        RefSolver.setA(RefA);
        var x = new DMatrixRMaj(3, 1);
        RefSolver.solve(b, x);
        var RefDecomp = (LUDecompositionBase_DDRM) RefSolver.getDecomposition();
        var RefLU = RefDecomp.getLU();

        var LU = new double[9];
        var pvt = new int[3];

        System.out.println(x);
//        new PowerGridNative().factorize(RefA.data, LU, pvt);

        System.out.println(RefLU);
        System.out.println(Arrays.toString(RefDecomp.getPivot()));
        System.out.println(Arrays.toString(LU));
        System.out.println(Arrays.toString(pvt));

        var net = new ElectricalNetwork(false);
        var n1 = new FloatingNode();
        var n2 = new FloatingNode();
        net.addNode(new VoltageSourceCoupling(n1, n2, 1, 1));
        net.addNode(n1);
        net.addNode(n2);
        net.calculate(1);
    }
}
