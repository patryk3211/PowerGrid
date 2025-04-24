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

public class TransformerTests extends TestHelper {
    @Test
    void transformer1P1S() {
        var Net = new Network();

        var V1 = Net.V(5);
        var Sec = Net.N();

        Net.TR(2, V1, Sec);

        Net.W(10, Sec, null);

        Net.calculate();

        Assertions.assertEquals(5 * 2, Sec.getVoltage(), 1e-6, "Transformer secondary has incorrect voltage");
        Assertions.assertEquals((double) (5 * 2) / 10 * 2, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
    }

    @Test
    void transformer1P2S() {
        var Net = new Network();

        var V1 = Net.V(5);
        var S1 = Net.N();
        var S2 = Net.N();

        Net.TR(1, V1, S1, S2);

        var Load = Net.W(10, S1, null);

        Net.calculate();

        Assertions.assertEquals(0, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
        Assertions.assertEquals(0, S1.getVoltage(), 2e-6, "Transformer secondary has incorrect voltage");
        Assertions.assertEquals(-5, S2.getVoltage(), 2e-6, "Transformer secondary has incorrect voltage");

        Load.remove();
        Load = Net.W(10, S2, null);

        Net.calculate();

        Assertions.assertEquals(0, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
        Assertions.assertEquals(5, S1.getVoltage(), 2e-6, "Transformer secondary has incorrect voltage");
        Assertions.assertEquals(0, S2.getVoltage(), 2e-6, "Transformer secondary has incorrect voltage");

        Load.remove();
        Load = Net.W(10, S1, S2);

        Net.calculate();

        Assertions.assertEquals(5f / 10, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
        Assertions.assertEquals(5, Math.abs(S1.getVoltage() - S2.getVoltage()), 1e-6, "Transformer secondary has incorrect voltage");
    }

    @Test
    void transformer2P2S() {
        var Net = new Network();

        var V1 = Net.V(5);
        var V2 = Net.V(2);

        var S1 = Net.N();
        var S2 = Net.N();

        Net.TR(2, V1, V2, S1, S2);

        Net.W(10, S1, S2);

        Net.calculate();

        Assertions.assertEquals((5 - 2) * 2, Math.abs(S1.getVoltage() - S2.getVoltage()), 1e-6, "Transformer secondary has incorrect voltage");
        Assertions.assertEquals(V1.getCurrent(), -V2.getCurrent(), 1e-6, "Voltage source currents are not balanced");
    }

    @Test
    void transformerImpedance() {
        var Net = new Network();

        var V1 = Net.V(5);
        var S1 = Net.N();
        var S2 = Net.N();

        Net.TR(2, 10, V1, S1, S2);

        Net.W(10, S1, S2);

        Net.calculate();

        Assertions.assertEquals(((5f * 2) / 20f) * 2, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");
        Assertions.assertEquals(5f, Math.abs(S1.getVoltage() - S2.getVoltage()), 1e-6, "Transformer secondary voltage is incorrect");
    }
}
