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
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;

public class SwitchTests {
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
    public void basicSwitchTest() {
        var network = new ElectricalNetwork();

        var V1 = V(5);
        var N1 = N();

        var Load = W(1, N1, null);
        var Switch = new SwitchedWire(1, V1, N1);

        network.addNodes(V1, N1);
        network.addWire(Load);
        network.addWire(Switch);

        network.calculate();

        Assertions.assertEquals(2.5f, N1.getVoltage(), 1e-6, "Incorrect voltage at node 1");
        Assertions.assertEquals(5.0f / 2f, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");

        Switch.setState(false);
        network.calculate();

        Assertions.assertEquals(0f, N1.getVoltage(), 1e-6, "Incorrect voltage at node 1");
        Assertions.assertEquals(0f, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
    }

    @Test
    public void initialStateTest() {
        var network = new ElectricalNetwork();

        var V1 = V(5);
        var N1 = N();

        var Load = W(1, N1, null);
        var Switch = new SwitchedWire(1, V1, N1, false);

        network.addNodes(V1, N1);
        network.addWire(Load);
        network.addWire(Switch);

        network.calculate();

        Assertions.assertEquals(0f, N1.getVoltage(), 1e-6, "Incorrect voltage at node 1");
        Assertions.assertEquals(0f, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");

        Switch.setState(true);
        network.calculate();

        Assertions.assertEquals(2.5f, N1.getVoltage(), 1e-6, "Incorrect voltage at node 1");
        Assertions.assertEquals(5.0f / 2f, V1.getCurrent(), 1e-6, "Voltage source has incorrect current");
    }
}
