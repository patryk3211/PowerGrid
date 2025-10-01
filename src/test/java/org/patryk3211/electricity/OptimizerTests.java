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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;

public class OptimizerTests extends TestHelper {
    private FloatingNode[] buildGrid(Network Net, int size) {
        var nodes = new FloatingNode[size * size];
        for(int i = 0; i < size * size; ++i) {
            nodes[i] = Net.N();
        }

        float r = 1.0f;
        for(int x = 0; x < size; ++x) {
            for(int y = 0; y < size; ++y) {
                var origin = nodes[x + y * size];
                if(x < size - 1) {
                    // Horizontal
                    Net.W(r, origin, nodes[x + 1 + y * size]);
                    r += 1.0f;
                }
                if(y < size - 1) {
                    // Vertical
                    Net.W(r, origin, nodes[x + (y + 1) * size]);
                    r += 1.0f;
                }
            }
        }

        return nodes;
    }

    @Test
    void optimizerLinearSimulationEquivalenceTest() {
        var Net = new Network();

        // Simulate a NxN grid of nodes connected by resistors
        final int SIZE = 4;
        var nodes = buildGrid(Net, SIZE);

        // Connect sources at corners
        var V1 = Net.V(5);
        var GND = Net.V(0);
        Net.W(0.1f, V1, nodes[0]);
        Net.W(0.1f, GND, nodes[SIZE * SIZE - 1]);

        for(int i = 0; i < 10; ++i) {
            V1.setVoltage(-V1.getVoltage());
            var start = System.nanoTime();
            Net.calculate();
            var end = System.nanoTime();
            var duration = end - start;
            System.out.printf("Solve took %.3fµs\n", duration / 1000.0);
        }

        System.out.println("Unoptimized result:");
        for(int y = 0; y < SIZE; ++y) {
            for(int x = 0; x < SIZE; ++x) {
                System.out.printf("%6.3f ", nodes[x + y * SIZE].getVoltage());
            }
            System.out.println();
        }

        // Simulate a NxN grid of nodes connected by resistors,
        // this time with optimization.
        var Net2 = new Network();
        var nodes2 = buildGrid(Net2, SIZE);

        // Connect sources at corners
        var V2 = Net2.V(5);
        var GND2 = Net2.V(0);
        Net2.W(0.1f, V2, nodes2[0]);
        Net2.W(0.1f, GND2, nodes2[SIZE * SIZE - 1]);

        Net2.calculate();

        System.out.println("Optimized result:");
        for(int y = 0; y < SIZE; ++y) {
            for(int x = 0; x < SIZE; ++x) {
                System.out.printf("%6.3f ", nodes2[x + y * SIZE].getVoltage());
            }
            System.out.println();
        }

        for(int i = 0; i < SIZE * SIZE; ++i) {
            Assertions.assertEquals(nodes[i].getVoltage(), nodes2[i].getVoltage(), 1e-6, "Optimized network solves with different results");
        }
    }
}
