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
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.*;

public class SolverTests {
    static FloatingNode N() {
        return new FloatingNode();
    }

    static VoltageSourceNode V(float voltage) {
        return new VoltageSourceNode(voltage);
    }

    static ElectricWire W(float R, IElectricNode N1, IElectricNode N2) {
        return new ElectricWire(R, N1, N2);
    }

    @Test
    void testResistorDivider() {
        var network = new ElectricalNetwork();

        var V1 = new VoltageSourceNode(5);

        var N1 = new FloatingNode();

        network.addNodes(V1, N1);

        network.addWire(new ElectricWire(10, V1, N1));
        network.addWire(new ElectricWire(20, N1, null));

        network.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");
    }

    @Test
    void testTwoTransformedSources() {
        var network = new ElectricalNetwork();

        VoltageSourceNode V1 = V(5), V2 = V(4);
        FloatingNode V1P = N(), V1N = N(), V2P = N(), V2N = N();

        CouplingNode V1C = TransformerCoupling.create(1, V1, V1P, V1N);
        CouplingNode V2C = TransformerCoupling.create(1, V2, V2P, V2N);

        network.addNodes(V1, V1P, V1N);
        network.addNode(V1C);

        network.addNodes(V2, V2P, V2N);
        network.addNode(V2C);

        network.addWire(W(5f, V1N, V2N));
        network.addWire(W(5f, V1P, V2P));

        network.calculate();

        Assertions.assertEquals( 1f / 10, V1.getCurrent(), 1e-6, "Voltage source 1 current is incorrect");
        Assertions.assertEquals(-1f / 10, V2.getCurrent(), 1e-6, "Voltage source 2 current is incorrect");
    }
}
