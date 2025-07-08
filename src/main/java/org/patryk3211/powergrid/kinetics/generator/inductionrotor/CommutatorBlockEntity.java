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
package org.patryk3211.powergrid.kinetics.generator.inductionrotor;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlockEntity;

import java.util.List;

import static net.minecraft.state.property.Properties.AXIS;

public class CommutatorBlockEntity extends RotorBlockEntity implements IElectricEntity {
    protected ElectricBehaviour electricBehaviour;
    protected ThermalBehaviour thermalBehaviour;
    protected SwitchedWire wire;
    private float resistance = 0;
    private int inductionRotorCount;

    public CommutatorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connectSwitch(Math.max(resistance, 1f), builder.terminalNode(0), builder.terminalNode(1), inductionRotorCount > 0);
    }

    private void assemblyChanged() {
        resistance = 0;
        inductionRotorCount = 0;
        rotorBehaviour.forEachSegment(segment -> {
            if(segment.blockEntity instanceof InductionRotorBlockEntity rotor) {
                resistance += rotor.getResistance();
                ++inductionRotorCount;
            }
        });
        if(inductionRotorCount > 0) {
            wire.setResistance(resistance);
            wire.setState(true);
        } else {
            wire.setState(false);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rotorBehaviour.noField();
        rotorBehaviour.setChangeCallback(this::assemblyChanged);

        electricBehaviour = new ElectricBehaviour(this);
        behaviours.add(electricBehaviour);

//        thermalBehaviour = specifyThermalBehaviour();
//        if(thermalBehaviour != null)
//            behaviours.add(thermalBehaviour);
    }

    protected void applyLostPower(float power) {
        if(thermalBehaviour != null)
            thermalBehaviour.applyTickPower(power);
    }

    @Override
    public void remove() {
        super.remove();
        if(electricBehaviour != null) {
            electricBehaviour.breakConnections();
        }
    }

    public float getCurrent() {
        return wire.current();
    }

    @Override
    public void tick() {
        super.tick();
        if(world.isClient) {
            var angular = rotorBehaviour.getAngularVelocityRadians();
            var current = getCurrent();
            float chance = Math.abs(angular / 32f * current / 4f);

            var r = world.random;
            while(chance > 0) {
                if(r.nextFloat() < chance) {
                    boolean secondBrush = r.nextBoolean();
                    var pos = getPos().toCenterPos();
                    var brushOffset = switch (getCachedState().get(AXIS)) {
                        case Z -> new Vec3d(3.5 / 16f, 0, 2 / 16f).addRandom(r, 1 / 16f);
                        case X -> new Vec3d(2 / 16f, 0, 3.5 / 16).addRandom(r, 1 / 16f);
                        case Y -> new Vec3d(3.5 / 16f, 2 / 16f, 0).addRandom(r, 1 / 16f);
                    };

                    pos = secondBrush ? pos.add(brushOffset) : pos.subtract(brushOffset);
                    int velocityDir = (angular < 0 ^ secondBrush) ? 1 : -1;
                    var velocity = switch (getCachedState().get(AXIS)) {
                        case X, Z -> new Vec3d(0, 1 / 4f + Math.abs(angular) / 100f, 0).addRandom(r, 1 / 16f);
                        case Y -> Vec3d.ZERO;
                    };

                    world.addParticle(new SparkParticleData(r.nextBetween(1, 3), false, true), pos.x, pos.y, pos.z,
                            velocity.x * velocityDir, velocity.y * velocityDir, velocity.z * velocityDir);
                }
                chance -= 1;
            }
        }
    }
}
