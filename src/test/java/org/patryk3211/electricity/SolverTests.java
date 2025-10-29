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
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.electricity.sim.special.GeneratorCoupling;
import org.patryk3211.powergrid.electricity.sim.special.IRotor;

public class SolverTests extends TestHelper {
    @Test
    void testResistorDivider() {
        var Net = new Network();

        var V1 = Net.V(5);
        var N1 = Net.N();

        Net.W(10.0f, V1, N1);
        Net.W(20.0f, N1, null);

        Net.calculate();

        Assertions.assertEquals(5f * 20 / (10 + 20), N1.getVoltage(), 1e-6, "Resistor divider node has incorrect voltage");
        Assertions.assertEquals(5f / 30, V1.getCurrent(), 1e-6, "Voltage source current is incorrect");
    }

    @Test
    void testTwoTransformedSources() {
        var Net = new Network();

        VoltageSourceNodePair V1 = Net.V(5), V2 = Net.V(4);
        FloatingNode V1P = Net.N(), V1N = Net.N(), V2P = Net.N(), V2N = Net.N();

        Net.TR(1, V1, V1P, V1N);
        Net.TR(1, V2, V2P, V2N);

        Net.W(5.0f, V1N, V2N);
        Net.W(5.0f, V1P, V2P);

        Net.calculate();

        Assertions.assertEquals( 1f / 10, V1.getCurrent(), 1e-6, "Voltage source 1 current is incorrect");
        Assertions.assertEquals(-1f / 10, V2.getCurrent(), 1e-6, "Voltage source 2 current is incorrect");
    }

    @Test
    void testTwoTerminalSource() {
        // Needs anchoring to converge
        var Net = new Network(true);

        var N1 = Net.N();
        var N2 = Net.N();
        var V = new VoltageSourceCoupling(N1, N2, 0, 5);
        Net.network.addNode(V);

        Net.W(1, N1, N2);

        Net.calculate();

        Assertions.assertEquals(-5, V.getCurrent(), 1e-6, "Voltage source current is incorrect");
    }

    @Test
    void testTwoTerminalSourceWithResistance() {
        // Needs anchoring to converge
        var Net = new Network(true);

        var N1 = Net.N();
        var N2 = Net.N();
        var V = new VoltageSourceCoupling(N1, N2, 1, 5);
        Net.network.addNode(V);

        Net.W(1, N1, N2);

        Net.calculate();

        Assertions.assertEquals(-2.5, V.getCurrent(), 1e-6, "Voltage source current is incorrect");
        Assertions.assertEquals(2.5, N1.getVoltage() - N2.getVoltage(), 1e-6, "Wire voltage is incorrect");
    }

    private static class Rotor implements IRotor {
        private float target;
        private float velocity;

        public Rotor(float velocity) {
            this.velocity = velocity;
            this.target = velocity;
        }

        @Override
        public float getInertia() {
            return 0.5f;
        }

        @Override
        public float getAngularVelocity() {
            return velocity;
        }

        @Override
        public void applyTickForce(float force) {
            this.velocity += force / getInertia() * 0.05f;
        }

        public float energy() {
            return velocity * velocity * getInertia() * 0.5f;
        }

        public void tick() {
            float Kp = 0;//0.85f;
            float deltaT = (target - velocity);
            if(target < 0)
                deltaT = -deltaT;
            deltaT = Math.max(0, deltaT);
            float maxForce = 1;
            float force = (Kp * deltaT) * 20f * getInertia();
            force = Math.min(Math.abs(force), maxForce) * Math.signum(target);
            velocity += force / 20f / getInertia();
        }
    }

    @Test
    void testGenerator() {
        var Net = new Network(true);

        var rotor1 = new Rotor(256f);
        var rotor2 = new Rotor(250f);

        var N1 = Net.N();
        var N2 = Net.N();
        var N3 = Net.N();
        var N4 = Net.N();
        var V1 = new GeneratorCoupling(N1, N2, 1, rotor1);
        var V2 = new GeneratorCoupling(N3, N4, 1, rotor2);
        V1.setField(100);
        V2.setField(100);
        Net.network.addNodes(V1, V2);

//        Net.W(10f, N1, N2);
        Net.W(.1f, N1, N3);
        Net.W(.1f, N2, N4);
//        Net.W(10f, N1, N2);

        for(int i = 0; i < 40; ++i) {
            rotor1.tick();
            rotor2.tick();

            var E1 = rotor1.energy();
            var E2 = rotor2.energy();
            Net.calculate();

            V1.tick(100);
            V2.tick(100);
            var deltaE1 = rotor1.energy() - E1;
            var deltaE2 = rotor2.energy() - E2;

            var Pe1 = V1.getVoltage() * -V1.getCurrent();
            var Pe2 = V2.getVoltage() * -V2.getCurrent();

            System.out.printf("i = %d:\n", i);
            System.out.printf("  ω1 = %g\n    I_gen1 = %g\n    ΔE_gen1 = %g\n    P_gen1 = %g, E = %g\n",
                    rotor1.getAngularVelocity(), V1.getCurrent(), deltaE1, Pe1, Pe1 * 0.05f);
            System.out.printf("  ω2 = %g\n    I_gen2 = %g\n    ΔE_gen2 = %g\n    P_gen2 = %g, E = %g\n",
                    rotor2.getAngularVelocity(), V2.getCurrent(), deltaE2, Pe2, Pe2 * 0.05f);
            System.out.printf("  E_system = %g\n", rotor1.energy() + rotor2.energy());
        }
    }
}
