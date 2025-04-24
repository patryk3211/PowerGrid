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

public class SwitchTests extends TestHelper {
    @Test
    public void basicSwitchTest() {
        var Net = new Network();

        var V1 = Net.V(5);
        var N1 = Net.N();

        Net.W(1, N1, null);
        var Switch = Net.SW(1, V1, N1);

        Net.calculate();

        Assertions.assertEquals(2.5f, N1.getVoltage(), 1e-6, "Incorrect voltage at node 1");
        Assertions.assertEquals(5.0f / 2f, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");

        Switch.setState(false);
        Net.calculate();

        Assertions.assertEquals(0f, N1.getVoltage(), 1e-6, "Incorrect voltage at node 1");
        Assertions.assertEquals(0f, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
    }

    @Test
    public void initialStateTest() {
        var Net = new Network();

        var V1 = Net.V(5);
        var N1 = Net.N();

        Net.W(1, N1, null);
        var Switch = Net.SW(1, V1, N1, false);

        Net.calculate();

        Assertions.assertEquals(0f, N1.getVoltage(), 1e-6, "Incorrect voltage at node 1");
        Assertions.assertEquals(0f, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");

        Switch.setState(true);
        Net.calculate();

        Assertions.assertEquals(2.5f, N1.getVoltage(), 1e-6, "Incorrect voltage at node 1");
        Assertions.assertEquals(5.0f / 2f, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
    }
}
