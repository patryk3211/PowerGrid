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
import org.patryk3211.electricity.node.FloatingNode;
import org.patryk3211.electricity.node.TransformerCoupling;
import org.patryk3211.electricity.node.VoltageSourceNode;

public class StructureChangeTests {
    @Test
    void testNetworkAddWire() {
        var network = new ElectricalNetwork();

        var V1 = new VoltageSourceNode(5);

        var N1 = new FloatingNode();

        network.addNodes(V1, N1);

        network.addWire(new ElectricWire(10, V1, N1));
        network.addWire(new ElectricWire(20, N1, null));

        network.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");

        network.addWire(new ElectricWire(20, N1, null));
        network.calculate();

        Assertions.assertEquals(5f * 10 / (10 + 10), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after addWire()");
        Assertions.assertEquals(5f / 20, V1.getCurrent(), 1e-6, "Voltage source current is incorrect after structure after addWire()");
    }

    @Test
    void testNetworkRemoveWire() {
        var network = new ElectricalNetwork();

        var V1 = new VoltageSourceNode(5);

        var N1 = new FloatingNode();

        network.addNodes(V1, N1);

        network.addWire(new ElectricWire(10, V1, N1));
        network.addWire(new ElectricWire(20, N1, null));
        var wire = new ElectricWire(20, N1, null);
        network.addWire(wire);

        network.calculate();

        Assertions.assertEquals(5f * 10 / (10 + 10), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 20, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");

        network.removeWire(wire);
        network.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after removeWire()");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect after structure after removeWire()");
    }

    @Test
    void testNetworkAddNode() {
        var network = new ElectricalNetwork();

        var V1 = new VoltageSourceNode(5);

        var N1 = new FloatingNode();

        network.addNodes(V1, N1);

        network.addWire(new ElectricWire(10, V1, N1));
        network.addWire(new ElectricWire(20, N1, null));

        network.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");

        var N2 = new FloatingNode();
        network.addNode(N2);
        network.addWire(new ElectricWire(100, V1, N2));
        network.addWire(new ElectricWire(100, N2, null));
        network.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after addNode()");
        Assertions.assertEquals(5f * 100 / (100 + 100), N2.getVoltage(), 1e-6, "Second resistor divider has incorrect voltage");
        Assertions.assertEquals(5f * (1.0 / 30 + 1.0 / 200), V1.getCurrent(), 1e-6, "Voltage source current is incorrect after structure after addNode()");
    }

    @Test
    void testResistanceChange() {
        var network = new ElectricalNetwork();

        var V1 = new VoltageSourceNode(5);

        var N1 = new FloatingNode();

        network.addNodes(V1, N1);

        var VR1 = new ElectricWire(10, V1, N1);
        var VR2 = new ElectricWire(20, N1, null);
        network.addWire(VR1);
        network.addWire(VR2);

        network.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / (10 + 20), V1.getCurrent(), 1e-6, "Voltage source current is incorrect");

        VR1.setResistance(20);
        network.calculate();

        Assertions.assertEquals(5f * 20 / (20 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after VR1 resistance change");
        Assertions.assertEquals(5f / (20 + 20), V1.getCurrent(), 1e-6, "Voltage source current is incorrect after VR1 resistance change");

        VR2.setResistance(10);
        network.calculate();

        Assertions.assertEquals(5f * 10 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after VR2 resistance change");
        Assertions.assertEquals(5f / (10 + 20), V1.getCurrent(), 1e-6, "Voltage source current is incorrect after VR2 resistance change");

        VR1.setResistance(10);
        VR2.setResistance(20);
        network.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after VR1 and VR2 change");
        Assertions.assertEquals(5f / (10 + 20), V1.getCurrent(), 1e-6, "Voltage source current is incorrect after VR1 and VR2 change");
    }

    @Test
    void testCouplingAdd() {
        var network = new ElectricalNetwork();

        var V1 = new VoltageSourceNode(5);
        var secondary = new FloatingNode();

        network.addNodes(V1, secondary);
        network.addWire(new ElectricWire(10f, secondary, null));
        network.addWire(new ElectricWire(10f, V1, null));

        network.calculate();

        Assertions.assertEquals(5f / 10f, V1.getCurrent(), "Voltage source current is incorrect");

        var coupling = TransformerCoupling.create(1, V1, secondary);
        network.addNode(coupling);

        network.calculate();

        Assertions.assertEquals(5f * (1 / 10f + 1 / 10f), V1.getCurrent(), "Voltage source current is incorrect");
    }

    @Test
    void testCouplingRemove() {
        var network = new ElectricalNetwork();

        var V1 = new VoltageSourceNode(5);
        var secondary = new FloatingNode();

        network.addNodes(V1, secondary);
        network.addWire(new ElectricWire(10f, secondary, null));
        network.addWire(new ElectricWire(10f, V1, null));

        var coupling = TransformerCoupling.create(1, V1, secondary);
        network.addNode(coupling);

        network.calculate();

        Assertions.assertEquals(5f * (1 / 10f + 1 / 10f), V1.getCurrent(), "Voltage source current is incorrect");

        network.removeNode(coupling);
        network.calculate();

        Assertions.assertEquals(5f / 10f, V1.getCurrent(), "Voltage source current is incorrect");
    }
}
