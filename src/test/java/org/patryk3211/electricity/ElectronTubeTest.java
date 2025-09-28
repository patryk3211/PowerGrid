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
import org.patryk3211.powergrid.electricity.sim.special.ElectronTubeWire;

public class ElectronTubeTest extends TestHelper {
    public static void main(String[] args) {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(-1.0f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        // kG1 = 1 / perveance
        var Tube = new ElectronTubeWire(10, 0.001f, 10, Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final var R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for(int i = 0; i < 10; ++i) {
            Net.calculate();

            System.out.printf("Iteration %d:\n", i);
            System.out.printf("Anode voltage: %f\n", Anode.getVoltage());
            System.out.printf("Cathode voltage: %f\n", Cathode.getVoltage());
            System.out.printf("Grid voltage: %f\n", Grid.getVoltage());
            System.out.printf("Tube current: %f\n", Tube.current());

            System.out.printf("V1 current: %f\n", V1.getCurrent());
            System.out.printf("V2 current: %f\n", V2.getCurrent());
            System.out.printf("GND current %f\n\n", GND.getCurrent());
        }
    }

    @Test
    void testSimpleTubeCutoff() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(-5f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = new ElectronTubeWire(10, 0.01f, 10, Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for(int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertEquals(0, V1.getCurrent(), 1e-3f, "Anode current is incorrect");
    }

    @Test
    void testSimpleTubeConduct() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(0);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = new ElectronTubeWire(10, 0.001f, 10, Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for(int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertEquals(0.011143f, V1.getCurrent(), 1e-3f, "Anode current is incorrect");
        Assertions.assertEquals(49.8885f, Anode.getVoltage(), 1e-3f, "Anode voltage is incorrect");
        Assertions.assertEquals(V1.getCurrent(), Tube.current(), 1e-6f, "Tube current is incorrect");
    }

    @Test
    void testTubeReverse() {
        var Net = new Network();

        var V1 = Net.V(-10f);
        var V2 = Net.V(0);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = new ElectronTubeWire(10, 0.001f, 10, Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        Assertions.assertEquals(0, V1.getCurrent(), 1e-3f, "Anode is conducting at reverse voltage");
    }

    @Test
    void testTubeSaturation() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(0);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = new ElectronTubeWire(2, 0.001f, 0.1f, Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for(int i = 0; i < 5; ++i)
            Net.calculate();

        // Should be about 120mA if saturation wasn't the limit
        Assertions.assertEquals(0.1f, V1.getCurrent(), 1e-3f, "Anode current is incorrect");
        Assertions.assertEquals(49.0f, Anode.getVoltage(), 1e-3f, "Anode voltage is incorrect");
        Assertions.assertEquals(V1.getCurrent(), Tube.current(), 1e-6f, "Tube current is incorrect");
    }
}
