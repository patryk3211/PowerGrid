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

public class TransformerSimulation {
    public static void main(String[] args) {
        var Net = new TestHelper.Network();

        var P1 = Net.N();
        var P2 = Net.N();
        var S1 = Net.N();
        var S2 = Net.N();

        final float primaryTurns = 100;
        final float secondaryTurns = primaryTurns * 3;
        final float couplingFactor = 0.99f;

        final float ratio = (float) secondaryTurns / primaryTurns;
        final float A_l = 0.001f;
        final float primaryInductance = primaryTurns * primaryTurns * A_l;
        final float secondaryInductance = secondaryTurns * secondaryTurns * A_l;

        final float mutualInductance = primaryInductance * couplingFactor;
        final float primaryStray = primaryInductance - mutualInductance;
        final float secondaryStray = secondaryInductance - ratio * ratio * mutualInductance;

        var Tnode = Net.N();
        var Pnode = Net.N();

        Net.W(primaryStray, P1, Tnode);
        Net.W(secondaryStray, Tnode, Pnode);
        Net.W(mutualInductance, Tnode, P2);
        Net.TR(ratio, Pnode, P2, S1, S2);

        var V1 = Net.V(5);
        var GND = Net.V(0);

        Net.W(0.01f, V1, P1);
        Net.W(0.01f, GND, P2);
//        Net.W(1f, S1, S2);

        Net.calculate();

        System.out.printf("V1 current: %f\n", V1.getCurrent());
        System.out.printf("Secondary voltage: %f\n", S1.getVoltage() - S2.getVoltage());
    }
}
