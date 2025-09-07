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
package org.patryk3211.electricity.generators;

import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;

public class Simulation {
    public static void main(String[] args) throws InterruptedException {
        var network = new ElectricalNetwork();
        var generator = new Generator(10.0f, 0.2f);

        var source = new VoltageSourceNode(10);
        var gnd = new VoltageSourceNode(0);
        network.addNodes(source, gnd);
        generator.addTo(network);

        network.addWire(new ElectricWire(0.001f, source, generator.positive));
        network.addWire(new ElectricWire(0.001f, gnd, generator.negative));

        System.out.println("Charging");
        final float timeStep = 1f / 20;
        for (int i = 0; i < 100; ++i) {
//            System.out.printf("i = %d:\n", i);

            generator.step(timeStep);
            network.calculate();
        }

        System.out.printf("  Generator voltage: %f\n", generator.voltage());
        System.out.printf("  Generator energy: %f\n", generator.rotorEnergy());
        System.out.printf("  Generator speed: %f\n", generator.speed);
        System.out.printf("  Source current: %f\n", source.getCurrent());

        source.setVoltage(0);

        System.out.println("Discharging");
        for (int i = 0; i < 40; ++i) {
//            System.out.printf("i = %d:\n", i);

            generator.step(timeStep);
            network.calculate();
        }

        System.out.printf("  Generator voltage: %f\n", generator.voltage());
        System.out.printf("  Generator energy: %f\n", generator.rotorEnergy());
        System.out.printf("  Generator speed: %f\n", generator.speed);
        System.out.printf("  Source current: %f\n", source.getCurrent());
    }
}
