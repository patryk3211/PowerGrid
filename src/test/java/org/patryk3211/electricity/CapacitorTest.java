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
import org.patryk3211.powergrid.electricity.sim.special.CapacitorWire;

public class CapacitorTest extends TestHelper {
    @Test
    void testCapacitorCharging() {
        var Net = new Network();

        var V1 = Net.V(1);
        var N1 = Net.N();

        Net.W(1f, V1, N1);
        var C = new CapacitorWire(1f, N1, null);
        Net.network.addWire(C);

        // Simulate for 1 second
        for(int i = 0; i < 21; ++i) {
            Net.calculate();
            Assertions.assertEquals(V1.getCurrent(), C.current(), 1e-5f, "Capacitor current is incorrect");
        }

        Assertions.assertEquals(0.632f, C.potentialDifference(), 0.01f, "Capacitor voltage is incorrect");
        Assertions.assertEquals(0.368f, V1.getCurrent(), 0.01f, "Voltage source current is incorrect");
    }

    @Test
    void testTwoTerminalCapacitor() {
        var Net = new Network();

        var V1 = Net.V(5);
        var V2 = Net.V(5);

        var N1 = Net.N();
        var N2 = Net.N();

        Net.W(0.5f, V1, N1);
        Net.W(0.5f, V2, N2);
        var C = new CapacitorWire(1f, N1, N2);
        Net.network.addWire(C);

        Net.calculate();
        Assertions.assertEquals(0, V1.getCurrent(), 0.01f, "First voltage source current is incorrect");
        Assertions.assertEquals(0, V2.getCurrent(), 0.01f, "Second voltage source current is incorrect");

        V2.setVoltage(4);

        // Simulate for 1 second
        for(int i = 0; i < 21; ++i) {
            Net.calculate();
        }

        Assertions.assertEquals(0.632f, C.potentialDifference(), 0.01f, "Capacitor voltage is incorrect");
        Assertions.assertEquals( 0.368f, V1.getCurrent(), 0.01f, "First voltage source current is incorrect");
        Assertions.assertEquals(-0.368, V2.getCurrent(), 0.01f, "Second voltage source current is incorrect");
    }

    @Test
    void testFloating() {
        var Net = new Network();

        var N1 = Net.N();

        var C = new CapacitorWire(1f, N1, null);
        Net.network.addWire(C);
        C.setVoltage(5f);

        for(int i = 0; i < 2; ++i) {
            Net.calculate();
            Assertions.assertEquals(5.0f, C.potentialDifference(), 0.01f, "Capacitor voltage is incorrect");
        }
    }
}
