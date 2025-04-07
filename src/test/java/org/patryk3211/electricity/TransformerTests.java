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
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.TransformerCoupling;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;

public class TransformerTests {
    @Test
    void transformer1P1S() {
        var network = new ElectricalNetwork();

        var V1 = new VoltageSourceNode(5);
        var secondary = new FloatingNode();

        var transformer = TransformerCoupling.create(2, V1, secondary);

        network.addNodes(V1, secondary);
        network.addNode(transformer);

        network.addWire(new ElectricWire(10, secondary, null));

        network.calculate();

        Assertions.assertEquals(5 * 2, secondary.getVoltage(), 1e-6, "Transformer secondary has incorrect voltage");
        Assertions.assertEquals((double) (5 * 2) / 10 * 2, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
    }

    @Test
    void transformer1P2S() {
        var network = new ElectricalNetwork();

        var V1 = new VoltageSourceNode(5);
        var secondary1 = new FloatingNode();
        var secondary2 = new FloatingNode();

        var transformer = TransformerCoupling.create(1, V1, secondary1, secondary2);

        network.addNodes(V1, secondary1, secondary2);
        network.addNode(transformer);

        var wire = new ElectricWire(10, secondary1, null);
        network.addWire(wire);

        network.calculate();

        Assertions.assertEquals(0, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
        Assertions.assertEquals(0, secondary1.getVoltage(), 2e-6, "Transformer secondary has incorrect voltage");
        Assertions.assertEquals(-5, secondary2.getVoltage(), 2e-6, "Transformer secondary has incorrect voltage");

        network.removeWire(wire);
        wire = new ElectricWire(10, secondary2, null);
        network.addWire(wire);

        network.calculate();

        Assertions.assertEquals(0, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
        Assertions.assertEquals(5, secondary1.getVoltage(), 2e-6, "Transformer secondary has incorrect voltage");
        Assertions.assertEquals(0, secondary2.getVoltage(), 2e-6, "Transformer secondary has incorrect voltage");

        network.removeWire(wire);
        wire = new ElectricWire(10, secondary1, secondary2);
        network.addWire(wire);

        network.calculate();

        Assertions.assertEquals(5f / 10, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
        Assertions.assertEquals(5, Math.abs(secondary1.getVoltage() - secondary2.getVoltage()), 1e-6, "Transformer secondary has incorrect voltage");
    }

    @Test
    void transformer2P2S() {
        var network = new ElectricalNetwork();

        var V1 = new VoltageSourceNode(5);
        var V2 = new VoltageSourceNode(2);

        var S1 = new FloatingNode();
        var S2 = new FloatingNode();

        network.addNodes(V1, V2, S1, S2);

        var transformer = TransformerCoupling.create(2.0f, V1, V2, S1, S2);
        network.addNode(transformer);

        network.addWire(new ElectricWire(10.0f, S1, S2));

        network.calculate();

        Assertions.assertEquals((5 - 2) * 2, Math.abs(S1.getVoltage() - S2.getVoltage()), 1e-6, "Transformer secondary has incorrect voltage");
        Assertions.assertEquals(V1.getCurrent(), -V2.getCurrent(), 1e-6, "Voltage source currents are not balanced");
    }
}
