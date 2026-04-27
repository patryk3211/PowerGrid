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
import org.patryk3211.powergrid.electricity.sim.special.ColdCathodeRegulatorTubeWire;

public class ColdCathodeTubeTests extends TestHelper {
    public static void main(String[] args) {
        var Net = new Network();

        var V1 = Net.V(120f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();

        var Tube = new ColdCathodeRegulatorTubeWire(90, 60, 0.001f, 0.0025f, Anode, Cathode);
        Net.network.addWire(Tube);

        final var R = 0.001f;
        Net.W(33000f, V1, Anode);
        Net.W(R, GND, Cathode);
        Net.W(100000, Anode, Cathode);

        for(int i = 0; i < 10; ++i) {
            if(i == 5)
                V1.setVoltage(180f);
            Net.calculate();

            System.out.printf("Iteration %d:\n", i);
            System.out.printf("Anode voltage: %f\n", Anode.getVoltage());
            System.out.printf("Cathode voltage: %f\n", Cathode.getVoltage());
            System.out.printf("Tube Conductance: %f\n", Tube.conductance());
            System.out.printf("Tube Current: %f\n", Tube.current());

            System.out.printf("V1 current: %f\n", V1.getCurrent());
            System.out.printf("GND current %f\n\n", GND.getCurrent());
        }
    }

    @Test
    void testBelowBreakdown() {
        var Net = new Network();

        var V1 = Net.V(80);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();

        var Tube = new ColdCathodeRegulatorTubeWire(90, 60, 0.001f, 0.001f, Anode, Cathode);
        Net.network.addWire(Tube);

        final var R = 0.001f;
        Net.W(R, V1, Anode);
        Net.W(R, GND, Cathode);

        for(int i = 0; i < 3; ++i)
            Net.calculate();

        Assertions.assertEquals(80, Anode.getVoltage(), 1e-6, "Anode voltage is not correct");
        Assertions.assertEquals(0, V1.getCurrent(), 1e-4, "Source current is not correct");
        Assertions.assertEquals(V1.getCurrent(), Tube.current(), 1e-6, "Source current is not correct");
    }

    @Test
    void testBreakdown() {
        var Net = new Network();

        var V1 = Net.V(120);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();

        var Tube = new ColdCathodeRegulatorTubeWire(90, 60, 0.001f, 0.025f, Anode, Cathode);
        Net.network.addWire(Tube);

        final var R = 0.001f;
        Net.W(33000, V1, Anode);
        Net.W(R, GND, Cathode);

        for(int i = 0; i < 3; ++i)
            Net.calculate();

        Assertions.assertEquals(60.0, Anode.getVoltage(), 0.25, "Anode voltage is not correct");
        Assertions.assertEquals(V1.getCurrent(), Tube.current(), 1e-6, "Tube current is not correct");
    }
}
