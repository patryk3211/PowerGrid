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
import org.patryk3211.powergrid.electricity.sim.special.BarretterWire;

public class BarretterTests extends TestHelper {
    @Test
    void simpleBarretterTest() {
        var Net = new Network();

        var V1 = Net.V(120);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();

        var Tube = new BarretterWire(0.5f, 10f, Anode, Cathode);
        Net.network.addWire(Tube);

        final var R = 0.001f;
        var Load = Net.W(0.1f, V1, Anode);
        Net.W(R, GND, Cathode);

        for(int i = 0; i < 3; ++i)
            Net.calculate();

        Assertions.assertEquals(0.5f, Load.current(), 1e-3f, "Load current is not correct");
        Assertions.assertEquals(V1.getCurrent(), Tube.current(), 1e-6f, "Tube current is not correct");

        V1.setVoltage(130);

        for(int i = 0; i < 3; ++i)
            Net.calculate();

        Assertions.assertEquals(0.5f, Load.current(), 1e-3f, "Load current is not correct");
        Assertions.assertEquals(V1.getCurrent(), Tube.current(), 1e-6f, "Tube current is not correct");
    }
}
