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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlockEntity;

import java.util.List;

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
    public float inertia() {
        return ModdedConfigs.server().kinetics.generatorControls.generatorCommutatorInertia.getF();
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
                resistance += ResistanceValues.get(rotor.getBlockState().getBlock());
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

    protected void applyPower(AbstractElectricWire wire) {
        if(thermalBehaviour != null)
            thermalBehaviour.applyWirePower(wire);
    }

    @Override
    public void remove() {
        super.remove();
        if(electricBehaviour != null) {
            electricBehaviour.remove();
        }
    }

    public float getCurrent() {
        return wire.current();
    }

    @Override
    public void tick() {
        super.tick();
        if(level.isClientSide) {
            var angular = rotorBehaviour.getAngularVelocityRadians();
            var current = getCurrent();
            // Max 5 particles per tick
            float chance = Math.min(Math.abs(angular / 32f * current / 4f), 5);

            if(!(getBlockState().getBlock() instanceof IBrushPlacement brushes))
                return;

            var r = level.random;
            while(chance > 0) {
                if(r.nextFloat() < chance) {
                    boolean secondBrush = r.nextBoolean();
                    var pos = getBlockPos().getCenter();
                    var brushOffset = brushes.brushOffset(getBlockState()).offsetRandom(r, 1 / 16f);

                    pos = secondBrush ? pos.add(brushOffset) : pos.subtract(brushOffset);
                    int velocityDir = (angular < 0 ^ secondBrush) ? 1 : -1;
                    var velocity = brushes.sparkVelocity(getBlockState(), angular).offsetRandom(r, 1 / 16f);

                    level.addParticle(new SparkParticleData(r.nextIntBetweenInclusive(1, 3), false, true), pos.x, pos.y, pos.z,
                            velocity.x * velocityDir, velocity.y * velocityDir, velocity.z * velocityDir);
                }
                chance -= 1;
            }
        }
    }
}
