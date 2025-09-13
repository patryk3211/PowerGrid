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
import org.patryk3211.powergrid.electricity.sim.special.TransistorNPN;

public class TransistorTest {
    @Test
    void simpleBJTTest() {
        var Net = new TestHelper.Network();

        var V1 = Net.V(10.0f);
        var VB = Net.V(0.8f);
        var GND = Net.V(0);

        var Collector = Net.N();
        var Base = Net.N();
        var Emitter = Net.N();

        var T = new TransistorNPN(Collector, Emitter, Base, 0.7f, 10, 15);

        Net.W(1.0f, V1, Collector);
        Net.W(1.0f, VB, Base);
        Net.W(1.0f, GND, Emitter);
        Net.network.addWire(T);

        for(int i = 0; i < 10; ++i) {
            Net.calculate();

            System.out.printf("Iteration %d:\n", i);
            System.out.printf("Collector voltage: %f\n", Collector.getVoltage());
            System.out.printf("Base voltage: %f\n", Base.getVoltage());
            System.out.printf("Emitter voltage: %f\n", Emitter.getVoltage());

            System.out.printf("T Current: %f\n", T.current());

            System.out.printf("V1 current: %f\n", V1.getCurrent());
            System.out.printf("VB current: %f\n", VB.getCurrent());
            System.out.printf("GND current %f\n\n", GND.getCurrent());

//            if(i == 5)
//                V1.setVoltage(0);
        }
    }
}
