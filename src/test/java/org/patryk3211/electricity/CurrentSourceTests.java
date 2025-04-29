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

public class CurrentSourceTests extends TestHelper {
    @Test
    void testCurrentSource() {
        var Net = new Network();

        var C1 = Net.C(1);
        Net.W(1, C1, null);

        Net.calculate();

        Assertions.assertEquals(1f, C1.getVoltage(), 1e-6, "Current source voltage is incorrect");

        C1.setCurrent(2);
        Net.calculate();

        Assertions.assertEquals(2f, C1.getVoltage(), 1e-6, "Current source voltage is incorrect");
    }

    @Test
    void testCurrentSourceDivider() {
        var Net = new Network();

        var C1 = Net.C(1);
        var N1 = Net.N();

        Net.W(1, C1, N1);
        Net.W(1, N1, null);

        Net.calculate();

        Assertions.assertEquals(2f, C1.getVoltage(), 1e-6, "Current source voltage is incorrect");
        Assertions.assertEquals(1f, N1.getVoltage(), 1e-6, "Divider voltage is incorrect");
    }

    @Test
    void transformerCurrentSource() {
        var Net = new Network();

        var C1 = Net.C(1);
        var N1 = Net.N();
        var S1 = Net.N();
        var S2 = Net.N();

        Net.TR(2, N1, S1, S2);

        Net.W(1, C1, N1);
        var Load = Net.W(4, S1, S2);

        Net.calculate();

        Assertions.assertEquals(2.0f, C1.getVoltage(), 1e-6, "Current source has incorrect voltage");
        Assertions.assertEquals(2.0f, S1.getVoltage() - S2.getVoltage(), 1e-6, "Transformer secondary has incorrect voltage");
    }

    @Test
    void transformerWithImpedanceAndCurrentSource() {
        var Net = new Network();

        var C1 = Net.C(1);
        var S1 = Net.N();
        var S2 = Net.N();

        Net.TR(1, 1, C1, S1, S2);

        var Load = Net.W(1, S1, S2);

        Net.calculate();

        Assertions.assertEquals(2.0f, C1.getVoltage(), 1e-6, "Current source has incorrect voltage");
        Assertions.assertEquals(1.0f, S1.getVoltage() - S2.getVoltage(), 1e-6, "Transformer secondary has incorrect voltage");
    }
}
