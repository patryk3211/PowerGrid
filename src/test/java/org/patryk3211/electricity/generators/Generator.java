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

import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.*;
/*
 * Magnetic flux = Loop area * Field strength
 * Electromotive force (voltage) = Magnetic flux * Speed
 *
 * Lorentz force = Loop length * Current * Field strength
 */
public class Generator {
    private static final float RESISTANCE = 10;

    public float speed;
    private float inertia;
    private float fieldStrength;
    private float loopArea;
    private float loopLength;
    private float turns;

    private final VoltageSourceNode source;
    private final ICouplingNode coupling;
    public final IElectricNode positive;
    public final IElectricNode negative;

    public Generator(float inertia, float fieldStrength) {
        this.inertia = inertia;
        this.speed = 0.0f;
        this.fieldStrength = fieldStrength;

        float r = 2;
        this.turns = 5;
        this.loopArea = (float) (Math.PI * r * r);
        this.loopLength = (float) (Math.PI * 2 * r) * turns;

        source = new VoltageSourceNode();
        positive = new FloatingNode();
        negative = new FloatingNode();
        coupling = TransformerCoupling.create(1, RESISTANCE, source, positive, negative);
    }

    public void addTo(ElectricalNetwork network) {
        network.addNodes(source, positive, negative);
        network.addNode(coupling);
    }

    public float rotorEnergy() {
        return 0.5f * speed * speed * inertia;
    }

    // N * B * A = K_e (motor/generator electrical constant)
    public float voltage() {
        return -speed * turns * fieldStrength * loopArea;
    }

    public void step(float timeStep) {
        float current = source.getCurrent();
        float electricalPower = current * current * RESISTANCE;
        float emf = current * RESISTANCE;

        // This is the delta speed needed to achieve equilibrium.
//        float deltaSpeed = Math.abs(emf / (turns * fieldStrength * loopArea));

        // E_0 = 0.5 * V² * m
        // E_1 = 0.5 * V² * m + W
        // dE = W
        // V_1 = sqrt((0.5 * V² * m + W) * 2 / m)
        // dV = V_1 - V
        // sqrt((0.5 * V² * m + W) * 2 / m)
        // V_1 = sqrt(V² + 2W/m)
//        float maxDeltaSpeed = Math.abs(Math.abs(speed) - (float) Math.sqrt(speed * speed + 2.0f * electricalPower * timeStep / inertia));
        float torque = turns * fieldStrength * loopArea * current;

        float e0 = rotorEnergy();
        speed += torque / inertia * timeStep;
//        speed += Math.min(deltaSpeed, maxDeltaSpeed) * Math.signum(emf);
        float e1 = rotorEnergy();

//        System.out.printf("Energy delta: %f\n", e1 - e0);
//        System.out.printf("Electrical energy: %f\n", electricalPower * timeStep);

        source.setVoltage(voltage());
    }
}
