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

import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;

import java.util.ArrayList;

public class PerformanceTest extends TestHelper {
    public static void main(String[] args) {
        var Net1 = new Network();
        Net1.network.setSolverType(ElectricalNetwork.SolverType.GMRES);
        var rep1 = buildGridTestNetwork(Net1, "GMRES");

        var Net2 = new Network();
        Net2.network.setSolverType(ElectricalNetwork.SolverType.DIRECT);
        var rep2 = buildGridTestNetwork(Net2, "Direct");

        rep1.run();
        rep2.run();
    }

    private static Runnable buildGridTestNetwork(Network Net, String prefix) {
        // Simulate a NxN grid of nodes connected by resistors
        final int SIZE = 32; // 32 * 32 = 1024 nodes
        var wires = new ArrayList<ElectricWire>();
        var nodes = buildGrid(Net, SIZE, wires);

        // Connect sources at corners
        var V1 = Net.V(5);
        var GND = Net.V(0);
        Net.W(0.1f, V1, nodes[0]);
        Net.W(0.1f, GND, nodes[SIZE * SIZE - 1]);

        long micros = 0;
        for(int i = 0; i < 100; ++i) {
            V1.setVoltage(-V1.getVoltage());
            // Forces refactorization to happen
            wires.get(0).setResistance(wires.get(0).getResistance() * (i % 2 == 0 ? 2.0f : 0.5f));
            var start = System.nanoTime();
            Net.calculate();
            var end = System.nanoTime();
            var duration = end - start;
            if(i % 10 == 9)
                System.out.printf("[%s] (i=%d) Solve took %.3fµs\n", prefix, i, duration / 1000.0);
            micros += duration / 1000;
        }

        var start = System.nanoTime();
        for (int y = 0; y < SIZE; ++y) {
            for (int x = 0; x < SIZE; ++x) {
                nodes[x + y * SIZE].getVoltage();
            }
        }
        var end = System.nanoTime();

        var finalTime = micros;
        return () -> {
            System.out.printf("[%s] 100 solves took an average of %.3fµs per solve\n", prefix, finalTime / 100.0);
            System.out.printf("[%s] Result fetch took %.3fµs\n", prefix, (end - start) / 1000.0);
        };
    }
}
