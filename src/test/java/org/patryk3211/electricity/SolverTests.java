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
import org.patryk3211.powergrid.electricity.sim.node.*;

public class SolverTests extends TestHelper {
    @Test
    void testResistorDivider() {
        var Net = new Network();

        var V1 = Net.V(5);
        var N1 = Net.N();

        Net.W(10.0f, V1, N1);
        Net.W(20.0f, N1, null);

        Net.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");
    }

    @Test
    void testTwoTransformedSources() {
        var Net = new Network();

        VoltageSourceNode V1 = Net.V(5), V2 = Net.V(4);
        FloatingNode V1P = Net.N(), V1N = Net.N(), V2P = Net.N(), V2N = Net.N();

        Net.TR(1, V1, V1P, V1N);
        Net.TR(1, V2, V2P, V2N);

        Net.W(5.0f, V1N, V2N);
        Net.W(5.0f, V1P, V2P);

        Net.calculate();

        Assertions.assertEquals( 1f / 10, V1.getCurrent(), 1e-6, "Voltage source 1 current is incorrect");
        Assertions.assertEquals(-1f / 10, V2.getCurrent(), 1e-6, "Voltage source 2 current is incorrect");
    }
}
