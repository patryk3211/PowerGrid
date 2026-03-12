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
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePort;

public class TransmissionLineTest {
    @Test
    public void simpleLineTest() {
        var RefNet = new TestHelper.Network();

        final float Z = 0.5f;
        final float R1 = 10f, R2 = 20f, V = 5;

        var RefV1 = RefNet.V(V);
        var RefN1 = RefNet.N();

        RefNet.W(R1, RefV1, RefN1);
        RefNet.W(R2, RefN1, null);

        RefNet.calculate();

        var Net1 = new TestHelper.Network();
        var Net2 = new TestHelper.Network();

        var V1 = Net1.V(V);
        var N1 = Net1.N();
        var N2 = Net2.N();

        var T1 = new TransmissionLinePort(N1, Z, null);
        var T2 = new TransmissionLinePort(N2, Z, null);
        T1.other = T2;
        T2.other = T1;

        Net1.W(R1 - Z, V1, N1);
        Net1.network.addNode(T1);
        Net2.network.addNode(T2);
        Net2.W(R2, N2, null);

        for(int i = 0; i < 20; ++i) {
            double Iprev = V1.getCurrent(), Vprev = N2.getVoltage();
            Net1.calculate();
            Net2.calculate();

            System.out.printf("Source current = %g, target = %g\n", V1.getCurrent(), RefV1.getCurrent());
            System.out.printf("Node voltage = %g, target = %g\n", N2.getVoltage(), RefN1.getVoltage());
            System.out.printf("dI = %g, dV = %g\n", V1.getCurrent() - Iprev, N2.getVoltage() - Vprev);
        }

        Assertions.assertEquals(RefN1.getVoltage(), N2.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(RefV1.getCurrent(), V1.getCurrent(), 1e-6, "Voltage source current is incorrect");
    }
}
