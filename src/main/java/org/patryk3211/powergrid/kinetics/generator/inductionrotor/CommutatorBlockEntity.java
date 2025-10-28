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
import org.apache.commons.lang3.mutable.MutableObject;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ProxyElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.special.GeneratorCoupling;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlockEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommutatorBlockEntity extends RotorBlockEntity implements IElectricEntity {
    protected ElectricBehaviour electricBehaviour;
    protected ThermalBehaviour thermalBehaviour;
    protected GeneratorCoupling source;
    private float resistance = 0;
    private boolean updateBehaviour = true;
    private final Set<InductionRotorBlockEntity> rotors = new HashSet<>();

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
        source = builder.addInternalNode(GeneratorCoupling.class, builder.terminalNode(0), builder.terminalNode(1), resistance, rotorBehaviour);
    }

    private void assemblyChanged() {
        source = null;
        updateBehaviour = true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rotorBehaviour.setChangeCallback(this::assemblyChanged);

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
        return -source.getCurrent();
    }

    public float getPower() {
        return -source.getCurrent() * source.getVoltage();
    }

    @Override
    public void tick() {
        assert level != null;
        super.tick();
        if(updateBehaviour) {
            rotors.clear();
            resistance = 0;
            var proxyTarget = new MutableObject<BlockPos>(null);
            rotorBehaviour.forEachSegment(segment -> {
                if(segment.blockEntity instanceof InductionRotorBlockEntity rotor) {
                    resistance += ResistanceValues.get(rotor.getBlockState().getBlock());
                    rotors.add(rotor);
                } else if(segment.blockEntity instanceof CommutatorBlockEntity commutator) {
                    if(commutator.source != null) {
                        // Source already exists on a different block, this will be a proxy.
                        proxyTarget.setValue(commutator.worldPosition);
                    }
                }
            });
            if(electricBehaviour != null) {
                electricBehaviour.remove();
                removeBehaviour(ElectricBehaviour.TYPE);
            }
            if(proxyTarget.getValue() != null) {
                electricBehaviour = new ProxyElectricBehaviour(this, proxyTarget::getValue);
            } else {
                electricBehaviour = new ElectricBehaviour(this);
            }
            attachBehaviourLate(electricBehaviour);
            updateBehaviour = false;
        }
        float totalField = 0;
        if(source != null) {
            for (var rotor : rotors) {
                totalField += rotor.calculateField();
            }
            source.tick(totalField);
        }
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
