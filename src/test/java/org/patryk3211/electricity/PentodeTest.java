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
import org.patryk3211.powergrid.electricity.sim.special.PentodeWire;

public class PentodeTest extends TestHelper {
    @Test
    void testPentodeConducts() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(0);
        var VScreen = Net.V(50f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();
        var Screen = Net.N();

        var Tube = new PentodeWire(8, 6_000f, 4_500f, 48, 12, 1.35f, 10f, Cathode, Anode, Grid, Screen);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, VScreen, Screen);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertTrue(V1.getCurrent() > 1e-4f, "Anode should conduct with screen biased");
        Assertions.assertTrue(Anode.getVoltage() > 0, "Anode voltage should be positive");
        Assertions.assertTrue(VScreen.getCurrent() > 0, "Screen should draw current");
    }

    @Test
    void testPentodeCutoff() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(-20f);
        var VScreen = Net.V(50f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();
        var Screen = Net.N();

        var Tube = new PentodeWire(8, 60_000f, 4_500f, 48, 12, 1.35f, 10f, Cathode, Anode, Grid, Screen);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, VScreen, Screen);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertEquals(0, V1.getCurrent(), 1e-3f, "Anode current should be near zero when cut off");
    }

    @Test
    void testPentodeNeedsScreen() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(0);
        var VScreen = Net.V(0f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();
        var Screen = Net.N();

        var Tube = new PentodeWire(8, 6_000f, 4_500f, 48, 12, 1.35f, 10f, Cathode, Anode, Grid, Screen);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, VScreen, Screen);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertEquals(0, V1.getCurrent(), 1e-3f, "Anode should not conduct without screen voltage");
    }

    @Test
    void testPentodeReverse() {
        var Net = new Network();

        var V1 = Net.V(-10f);
        var V2 = Net.V(0);
        var VScreen = Net.V(50f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();
        var Screen = Net.N();

        var Tube = new PentodeWire(8, 6_000f, 4_500f, 48, 12, 1.35f, 10f, Cathode, Anode, Grid, Screen);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, VScreen, Screen);
        Net.W(R, GND, Cathode);

        Net.calculate();

        Assertions.assertEquals(0, V1.getCurrent(), 1e-3f, "Anode should not conduct at reverse voltage");
    }

    @Test
    void testPentodeSaturation() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(0);
        var VScreen = Net.V(50f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();
        var Screen = Net.N();

        var Tube = new PentodeWire(8, 6_000f, 4_500f, 48, 12, 1.35f, 0.1f, Cathode, Anode, Grid, Screen);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, VScreen, Screen);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertTrue(0.1f >= V1.getCurrent(), "Anode current should not exceed saturation");
        Assertions.assertTrue(49.0f <= Anode.getVoltage(), "Anode voltage should stay high under saturation");
    }
}
