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
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.special.ThyratronWire;

public class ThyratronTest extends TestHelper {
    private static final float MU = 20f;
    private static final float VB = 60f;
    private static final float VH = 12f;
    private static final float IH = 0.005f;
    private static final float G_ON = 0.05f;

    private static ThyratronWire tube(IElectricNode cathode, IElectricNode anode, IElectricNode grid) {
        return new ThyratronWire(MU, VB, VH, IH, G_ON, cathode, anode, grid);
    }

    @Test
    void testStrikeVoltage() {
        var Net = new Network();
        var cathode = Net.N();
        var anode = Net.N();
        var grid = Net.N();
        var Tube = tube(cathode, anode, grid);

        Assertions.assertEquals(60, Tube.strikeVoltage(0), 1e-6, "Strike at Vg=0 should equal Vb0");
        Assertions.assertEquals(160, Tube.strikeVoltage(-5), 1e-6, "Negative grid should raise strike voltage");
        Assertions.assertEquals(40, Tube.strikeVoltage(1), 1e-6, "Positive grid should lower strike voltage");
        Assertions.assertEquals(VH, Tube.strikeVoltage(10), 1e-6, "Strike voltage should not fall below holding voltage");
    }

    @Test
    void testHoldOff() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(-5f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = tube(Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertFalse(Tube.isLit(), "Tube should not fire when grid holds it off");
        Assertions.assertEquals(0, V1.getCurrent(), 1e-3f, "Anode current should be near zero when held off");
    }

    @Test
    void testFireWithGridPulse() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(1f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = tube(Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertTrue(Tube.isLit(), "Positive grid should lower strike voltage and fire the tube");
        Assertions.assertTrue(V1.getCurrent() > 0.1f, "Anode should conduct after firing");
        Assertions.assertEquals(V1.getCurrent(), Tube.current(), 1e-3f, "Tube current should match source current");
    }

    @Test
    void testLatchAfterGridRemoved() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(1f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = tube(Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertTrue(Tube.isLit(), "Tube should fire before the latch check");

        V2.setVoltage(-20f);
        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertTrue(Tube.isLit(), "Grid should lose control after firing");
        Assertions.assertTrue(V1.getCurrent() > 0.1f, "Anode should stay conducting after the grid is driven negative");
    }

    @Test
    void testExtinguishOnLowCurrent() {
        var Net = new Network();

        var V1 = Net.V(50f);
        var V2 = Net.V(1f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = tube(Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertTrue(Tube.isLit(), "Tube should fire before extinguishing");

        V1.setVoltage(0f);
        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertFalse(Tube.isLit(), "Tube should extinguish when anode current collapses");
        Assertions.assertEquals(0, V1.getCurrent(), 1e-3f, "Anode current should be near zero after extinguishing");
    }

    @Test
    void testReverse() {
        var Net = new Network();

        var V1 = Net.V(-50f);
        var V2 = Net.V(1f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = tube(Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertFalse(Tube.isLit(), "Thyratron should not fire with reverse anode voltage");
        Assertions.assertEquals(0, V1.getCurrent(), 1e-3f, "Anode should not conduct in reverse");
    }

    @Test
    void testColdCathodeDoesNotFire() {
        var Net = new Network();

        var V1 = Net.V(100f);
        var V2 = Net.V(0f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = tube(Cathode, Anode, Grid);
        Tube.setEmission(0);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertFalse(Tube.isLit(), "Cold cathode should not ignite the gas");
        Assertions.assertEquals(0, V1.getCurrent(), 1e-3f, "Anode current should be near zero when cold");
    }

    @Test
    void testSelfTriggerAtHighAnode() {
        var Net = new Network();

        var V1 = Net.V(100f);
        var V2 = Net.V(0f);
        var GND = Net.V(0);

        var Anode = Net.N();
        var Cathode = Net.N();
        var Grid = Net.N();

        var Tube = tube(Cathode, Anode, Grid);
        Net.network.addWire(Tube);

        final float R = 0.001f;
        Net.W(10f, V1, Anode);
        Net.W(R, V2, Grid);
        Net.W(R, GND, Cathode);

        for (int i = 0; i < 5; ++i)
            Net.calculate();

        Assertions.assertTrue(Tube.isLit(), "Anode voltage above Vb0 at Vg=0 should self-trigger");
        Assertions.assertTrue(V1.getCurrent() > 0.1f, "Anode should conduct after self-trigger");
    }
}
