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
import org.patryk3211.powergrid.electricity.sim.special.InductorWire;

public class InductorTest {
    @Test
    void testInductor() {
        var Net = new TestHelper.Network();

        var V1 = Net.V(5);
        var V2 = Net.V(5);

        var N1 = Net.N();
        var N2 = Net.N();

        Net.W(0.5f, V1, N1);
        Net.W(0.5f, V2, N2);
        var L = new InductorWire(1f, N1, N2);
        Net.network.addWire(L);

        Net.calculate();
        Assertions.assertEquals(0, V1.getCurrent(), 0.01f, "First voltage source current is incorrect");
        Assertions.assertEquals(0, V2.getCurrent(), 0.01f, "Second voltage source current is incorrect");

        V2.setVoltage(4);

        // Simulate for 1 second
        for(int i = 0; i < 20; ++i) {
            Net.calculate();
        }

        Assertions.assertEquals( 0.632, V1.getCurrent(), 0.01f, "First voltage source current is incorrect");
        Assertions.assertEquals(-0.632, V2.getCurrent(), 0.01f, "Second voltage source current is incorrect");
        Assertions.assertEquals(V1.getCurrent(), L.current(), 1e-6f, "Inductor current is incorrect");
    }
}
