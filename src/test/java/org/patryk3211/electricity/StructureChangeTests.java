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

public class StructureChangeTests extends TestHelper {
    @Test
    void testNetworkAddWire() {
        var Net = new Network();

        var V1 = Net.V(5);
        var N1 = Net.N();

        Net.W(10, V1, N1);
        Net.W(20, N1, null);

        Net.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");

        Net.W(20, N1, null);
        Net.calculate();

        Assertions.assertEquals(5f * 10 / (10 + 10), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after addWire()");
        Assertions.assertEquals(5f / 20, V1.getCurrent(), 1e-6, "Voltage source current is incorrect after structure after addWire()");
    }

    @Test
    void testNetworkRemoveWire() {
        var Net = new Network();

        var V1 = Net.V(5);
        var N1 = Net.N();

        Net.W(10, V1, N1);
        Net.W(20, N1, null);

        var Wire = Net.W(20, N1, null);

        Net.calculate();

        Assertions.assertEquals(5f * 10 / (10 + 10), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 20, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");

        Wire.remove();
        Net.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after removeWire()");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect after structure after removeWire()");
    }

    @Test
    void testNetworkAddNode() {
        var Net = new Network();

        var V1 = Net.V(5);
        var N1 = Net.N();

        Net.W(10, V1, N1);
        Net.W(20, N1, null);

        Net.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");

        var N2 = Net.N();
        Net.W(100, V1, N2);
        Net.W(100, N2, null);
        Net.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after addNode()");
        Assertions.assertEquals(5f * 100 / (100 + 100), N2.getVoltage(), 1e-6, "Second resistor divider has incorrect voltage");
        Assertions.assertEquals(5f * (1.0 / 30 + 1.0 / 200), V1.getCurrent(), 1e-6, "Voltage source current is incorrect after structure after addNode()");
    }

    @Test
    void testResistanceChange() {
        var Net = new Network();

        var V1 = Net.V(5);
        var N1 = Net.N();

        var VR1 = Net.W(10, V1, N1);
        var VR2 = Net.W(20, N1, null);

        Net.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / (10 + 20), V1.getCurrent(), 1e-6, "Voltage source current is incorrect");

        VR1.setResistance(20);
        Net.calculate();

        Assertions.assertEquals(5f * 20 / (20 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after VR1 resistance change");
        Assertions.assertEquals(5f / (20 + 20), V1.getCurrent(), 1e-6, "Voltage source current is incorrect after VR1 resistance change");

        VR2.setResistance(10);
        Net.calculate();

        Assertions.assertEquals(5f * 10 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after VR2 resistance change");
        Assertions.assertEquals(5f / (10 + 20), V1.getCurrent(), 1e-6, "Voltage source current is incorrect after VR2 resistance change");

        VR1.setResistance(10);
        VR2.setResistance(20);
        Net.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage after VR1 and VR2 change");
        Assertions.assertEquals(5f / (10 + 20), V1.getCurrent(), 1e-6, "Voltage source current is incorrect after VR1 and VR2 change");
    }

    @Test
    void testCouplingAdd() {
        var Net = new Network();

        var V1 = Net.V(5);
        var S = Net.N();

        Net.W(10, V1, null);
        Net.W(10, S, null);

        Net.calculate();

        Assertions.assertEquals(5f / 10f, V1.getCurrent(), "Voltage source current is incorrect");

        Net.TR(1, V1, S);

        Net.calculate();

        Assertions.assertEquals(5f * (1 / 10f + 1 / 10f), V1.getCurrent(), "Voltage source current is incorrect");
    }

    @Test
    void testCouplingRemove() {
        var Net = new Network();

        var V1 = Net.V(5);
        var S = Net.N();

        Net.W(10, V1, null);
        Net.W(10, S, null);

        var TR = Net.TR(1, V1, S);

        Net.calculate();

        Assertions.assertEquals(5f * (1 / 10f + 1 / 10f), V1.getCurrent(), "Voltage source current is incorrect");

        Net.network.removeNode(TR);
        Net.calculate();

        Assertions.assertEquals(5f / 10f, V1.getCurrent(), "Voltage source current is incorrect");
    }

    @Test
    void testVoltageSourceChange() {
        var Net = new Network();

        var V1 = Net.V(5);
        var N1 = Net.N();

        Net.W(10, V1, N1);
        Net.W(20, N1, null);

        Net.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");

        V1.setVoltage(10);

        Net.calculate();

        Assertions.assertEquals(10f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(10f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");

        V1.setVoltage(5);

        Net.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");
    }

    @Test
    void testMerge() {
        var Net1 = new Network();
        var V1 = Net1.V(5);
        var N1 = Net1.N();
        Net1.W(10, V1, N1);
        Net1.W(10, N1, null);

        var Net2 = new Network();
        var V2 = Net2.V(4);
        var N2 = Net2.N();
        Net2.W(10, V2, N2);
        Net2.W(10, N2, null);

        Net1.calculate();
        Net2.calculate();

        Net1.network.merge(Net2.network);

        N1.setVoltage(0);
        N2.setVoltage(0);
        V1.setCurrent(0);
        V2.setCurrent(0);
        Net1.calculate();

        Assertions.assertEquals(2.5f, N1.getVoltage(), "N1 voltage incorrect");
        Assertions.assertEquals(2f, N2.getVoltage(), "N2 voltage incorrect");
        Assertions.assertEquals(5f / 20, V1.getCurrent(), "V1 current incorrect");
        Assertions.assertEquals(4f / 20, V2.getCurrent(), "V2 current incorrect");
    }
}
