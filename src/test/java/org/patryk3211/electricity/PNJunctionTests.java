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

import org.junit.jupiter.api.Test;
import org.patryk3211.powergrid.electricity.sim.special.BJTWire;
import org.patryk3211.powergrid.electricity.sim.special.PNJunctionWire;

public class PNJunctionTests extends TestHelper {
    @Test
    void simplePNJunctionTest() {
        var Net = new Network();

        var V1 = Net.V(3.3f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();

        var D = new PNJunctionWire(5.47e-9, 0.0414f, 22, 1.783, Anode, Cathode);

        Net.W(10.1f, V1, Anode);
        Net.W(10.1f, GND, Cathode);
        Net.network.addWire(D);

        for(int i = 0; i < 10; ++i) {
            Net.calculate();

            System.out.printf("Iteration %d:\n", i);
            System.out.printf("Anode voltage: %f\n", Anode.getVoltage());
            System.out.printf("Cathode voltage: %f\n", Cathode.getVoltage());
            System.out.printf("Diode current: %f\n", D.current());

            System.out.printf("V1 current: %f\n", V1.getCurrent());
            System.out.printf("GND current %f\n\n", GND.getCurrent());
        }
    }

    @Test
    void simpleNPNTest() {
        var Net = new Network();

        var V1 = Net.V(3.3f);
        var V2 = Net.V(0.8f);
        var GND = Net.V(0);

        var Collector = Net.N();
        var Base = Net.N();
        var Emitter = Net.N();

        var T = new BJTWire(Collector, Base, Emitter, 5.47e-12, 10, 0.05);

        Net.W(10.0f, V1, Collector);
        Net.W(100.0f, V2, Base);
        Net.W(10.0f, GND, Emitter);
        Net.network.addWire(T);

        for(int i = 0; i < 20; ++i) {
            Net.calculate();

            System.out.printf("Iteration %d:\n", i);
            System.out.printf("Collector voltage: %f\n", Collector.getVoltage());
            System.out.printf("Base voltage: %f\n", Base.getVoltage());
            System.out.printf("Emitter voltage: %f\n", Emitter.getVoltage());

//            System.out.printf("Diode current: %f\n", D.current());

            System.out.printf("V1 current: %f\n", V1.getCurrent());
            System.out.printf("V2 current: %f\n", V2.getCurrent());
            System.out.printf("GND current %f\n\n", GND.getCurrent());
        }
    }
}
