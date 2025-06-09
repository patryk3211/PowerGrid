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
import org.patryk3211.powergrid.electricity.sim.TransmissionLine;

public class TransmissionLineTests extends TestHelper {
    @Test
    void testSimpleTransmissionLine() {
        var Net1 = new Network();
        var Net2 = new Network();

        var V1 = Net1.V(5);
        var GND = Net2.V(0);
        var N2 = Net2.N();

        var VTR1 = Net1.V(0);
        var VTR2 = Net2.V(0);

        var TL = new TransmissionLine();
        TL.setResistance(1.0f);
        TL.connectNode1(V1);
        TL.connectNode2(N2);

        Net2.W(5, N2, GND);

        for(int i = 0; i < 15; ++i) {
            Net1.calculate();
            Net2.calculate();
            TL.tick();

            System.out.printf("(i = %d):\n", i);
            System.out.printf("  TL1 Voltage: %f\n", TL.node1.getVoltage());
            System.out.printf("  TL2 Voltage: %f\n", TL.node2.getVoltage());
            System.out.printf("  N2 Voltage: %f\n", N2.getVoltage());
        }

        Assertions.assertEquals(4.166f, N2.getVoltage(), 1e-3, "Transmission line transferred voltage is invalid");
        Assertions.assertEquals(V1.getCurrent(), -GND.getCurrent(), 1e-3, "Transferred current is invalid");
        Assertions.assertEquals(0.833f, TL.potentialDifference(), 1e-3, "Transmission line has invalid potential difference");
    }
}
