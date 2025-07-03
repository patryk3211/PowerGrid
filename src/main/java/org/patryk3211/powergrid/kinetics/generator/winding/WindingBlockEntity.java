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
package org.patryk3211.powergrid.kinetics.generator.winding;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.TransformerCoupling;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;

import java.util.ArrayList;
import java.util.List;

import static org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock.*;

public class WindingBlockEntity extends ElectricBlockEntity {
    private WindingBlockEntity mainBE;
    private List<WindingBlockEntity> collectedBEs;
    private RotorBehaviour rotorP;
    private RotorBehaviour rotorN;

    private float coilConstant = 1;
    private float resistance = 0.1f;
    private int coilCount = 0;
    private VoltageSourceNode sourceNode;
    private TransformerCoupling coupling;

    public WindingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private boolean isMain() {
        return getCachedState().get(PART) == 0;
    }

    public float emfVoltage() {
        float voltage = 0;
        if(rotorP != null) {
            voltage -= rotorP.getAngularVelocityRadians() * rotorP.getFieldStrength() * coilConstant;
        }
        if(rotorN != null) {
            voltage -= rotorN.getAngularVelocityRadians() * rotorN.getFieldStrength() * coilConstant;
        }
        return voltage;
    }

    public float totalVoltage() {
        float total = 0;
        for(var be : mainBE.collectedBEs)
            total += be.emfVoltage();
        return total;
    }

    public void grabRotors() {
        if(world.isClient)
            return;
        var state = getCachedState();
        var along = state.get(ALONG_FIRST_AXIS);
        Direction.Axis magneticAxis = switch(state.get(AXIS)) {
            case X -> along ? Direction.Axis.Y : Direction.Axis.Z;
            case Y -> along ? Direction.Axis.Z : Direction.Axis.X;
            case Z -> along ? Direction.Axis.Y : Direction.Axis.X;
        };

        rotorP = BlockEntityBehaviour.get(world, pos.offset(magneticAxis, 1), RotorBehaviour.TYPE);
        if(rotorP != null && rotorP.blockEntity.getCachedState().get(AXIS) != state.get(AXIS))
            rotorP = null;
        rotorN = BlockEntityBehaviour.get(world, pos.offset(magneticAxis, -1), RotorBehaviour.TYPE);
        if(rotorN != null && rotorN.blockEntity.getCachedState().get(AXIS) != state.get(AXIS))
            rotorN = null;
        sendData();
    }

    @Override
    public void initialize() {
        var block = (WindingBlock) getCachedState().getBlock();
        if(isMain()) {
            mainBE = this;
            collectedBEs = new ArrayList<>();
            coilCount = 0;
            block.walk(world, pos, (pos1, state) -> {
                ++coilCount;
                collectedBEs.add(world.getBlockEntity(pos1, ModdedBlockEntities.WINDING.get()).orElseThrow());
            });
            resistance = coilCount * WindingBlock.resistance();
            coupling.setResistance(resistance);
        } else {
            mainBE = block.getMainBlockEntity(world, pos).orElseThrow();
        }
        super.initialize();
        grabRotors();
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if(clientPacket) {
            rotorP = null;
            rotorN = null;
            if(tag.contains("RotorP")) {
                var posArray = tag.getIntArray("RotorP");
                var pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
                rotorP = BlockEntityBehaviour.get(world, pos, RotorBehaviour.TYPE);
            }
            if(tag.contains("RotorN")) {
                var posArray = tag.getIntArray("RotorN");
                var pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
                rotorN = BlockEntityBehaviour.get(world, pos, RotorBehaviour.TYPE);
            }
        }
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(clientPacket) {
            if(rotorP != null) {
                var pos = rotorP.getPos();
                tag.putIntArray("RotorP", new int[] { pos.getX(), pos.getY(), pos.getZ() });
            }
            if(rotorN != null) {
                var pos = rotorN.getPos();
                tag.putIntArray("RotorN", new int[] { pos.getX(), pos.getY(), pos.getZ() });
            }
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        if(!isMain())
            return;
        builder.setTerminalCount(2);
        sourceNode = builder.addInternalNode(VoltageSourceNode.class);
        coupling = builder.couple(1, resistance, sourceNode, builder.terminalNode(0), builder.terminalNode(1));
    }

    public void onNeighborChanged(BlockPos neighborPos) {
        grabRotors();
    }

    public float windingCurrent() {
        return mainBE.sourceNode.getCurrent() / mainBE.coilCount;
    }

    @Override
    public void tick() {
        super.tick();

        if(rotorP != null) {
            float current = windingCurrent();
            float torque = coilConstant * rotorP.getFieldStrength() * current;

            float Pe = current * emfVoltage();
            if (Pe > 0) {
                // Generator is sourcing power
                // Apply more torque to account for losses, use
                // those losses to heat the coil up.
                torque *= 1.1f;
            } else {
                // Generator is sinking power
                // Reduce torque to account for losses
                torque *= 0.9f;
            }
//            float Pm = rotorP.getAngularVelocityRadians() * torque;
//            PowerGrid.LOGGER.info("Efficiency: {}", Pm / Pe);
//            PowerGrid.LOGGER.info("P_e: {}", current * emfVoltage());
//            PowerGrid.LOGGER.info("P_m: {}", rotor.getAngularVelocityRadians() * torque);

            rotorP.applyTickForce(torque);
        }
        if(rotorN != null) {
            float current = windingCurrent();
            float torque = coilConstant * rotorN.getFieldStrength() * current;

            float Pe = current * emfVoltage();
            if (Pe > 0) {
                // Generator is sourcing power
                // Apply more torque to account for losses, use
                // those losses to heat the coil up.
                torque *= 1.1f;
            } else {
                // Generator is sinking power
                // Reduce torque to account for losses
                torque *= 0.9f;
            }
//            float Pm = rotorN.getAngularVelocityRadians() * torque;
//            PowerGrid.LOGGER.info("Efficiency: {}", Pm / Pe);
//            PowerGrid.LOGGER.info("P_e: {}", current * emfVoltage());
//            PowerGrid.LOGGER.info("P_m: {}", rotor.getAngularVelocityRadians() * torque);

            rotorN.applyTickForce(torque);
        }

        if(isMain() && sourceNode != null)
            sourceNode.setVoltage(totalVoltage());
    }
}
