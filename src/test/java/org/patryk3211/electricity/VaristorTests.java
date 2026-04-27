/*
 * Copyright 2026 patryk3211
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
import org.patryk3211.powergrid.electricity.sim.special.VaristorWire;

public class VaristorTests extends TestHelper{
    @Test
    public void offTest() {
        var Net = new Network();

        var V1 = Net.V(100);
        var GND = Net.V(0);

        var N1 = Net.N();
        var N2 = Net.N();

        var VR = new VaristorWire(N1, N2, 0.00001, 200);
        Net.network.addWire(VR);

        final var R = 0.001f;
        var Load = Net.W(0.1f, V1, N1);
        Net.W(R, GND, N2);

        for(int i = 0; i < 5; ++i)
            Net.calculate();

        System.out.printf("I = %g\n", Load.current());
        Assertions.assertTrue(Load.current() < 0.002, "Load current is not correct");
        Assertions.assertEquals(V1.getCurrent(), VR.current(), 1e-6f, "Varistor current is not correct");
    }

    @Test
    public void voltage80percentTest() {
        var Net = new Network();

        var V1 = Net.V(160);
        var GND = Net.V(0);

        var N1 = Net.N();
        var N2 = Net.N();

        var VR = new VaristorWire(N1, N2, 0.00001, 200);
        Net.network.addWire(VR);

        final var R = 0.001f;
        var Load = Net.W(0.1f, V1, N1);
        Net.W(R, GND, N2);

        for(int i = 0; i < 10; ++i)
            Net.calculate();

        System.out.printf("I = %g\n", Load.current());
        Assertions.assertTrue(Load.current() < 0.1, "Load current is not correct");
        Assertions.assertEquals(V1.getCurrent(), VR.current(), 1e-6f, "Varistor current is not correct");
    }

    @Test
    public void shortTest() {
        var Net = new Network();

        var V1 = Net.V(250);
        var GND = Net.V(0);

        var N1 = Net.N();
        var N2 = Net.N();

        var VR = new VaristorWire(N1, N2, 0.00001, 200);
        Net.network.addWire(VR);

        final var R = 0.001f;
        var Load = Net.W(0.1f, V1, N1);
        Net.W(R, GND, N2);

        for(int i = 0; i < 10; ++i)
            Net.calculate();

        System.out.printf("I = %g\n", Load.current());
        Assertions.assertTrue(Load.current() > 5.000, "Load current is not correct");
    }
}
