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
package org.patryk3211.powergrid.kinetics.generator.coil;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlockEntity;

public class CoilBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<CoilBehaviour> TYPE = new BehaviourType<>("generator_coil");

    private final ICoilEntity coilEntity;
    private RotorBehaviour rotor;
    // Area * Number of turns
    private float coilConstant;

    public <T extends SmartBlockEntity&ICoilEntity> CoilBehaviour(T be) {
        super(be);
        coilEntity = be;
        coilConstant = 1;
    }

    @Override
    public void initialize() {
        super.initialize();
        grabRotor();
    }

    public void grabRotor() {
        var state = blockEntity.getCachedState();
        var facing = state.get(CoilBlock.FACING);
        rotor = get(getWorld(), getPos().offset(facing), RotorBehaviour.TYPE);
        blockEntity.sendData();
    }

    @Override
    public void onNeighborChanged(BlockPos neighborPos) {
        super.onNeighborChanged(neighborPos);

        var state = blockEntity.getCachedState();
        var facing = state.get(CoilBlock.FACING);
        if(neighborPos.equals(getPos().offset(facing))) {
            rotor = get(getWorld(), neighborPos, RotorBehaviour.TYPE);
            blockEntity.sendData();
        }
    }

    @Override
    public void read(NbtCompound nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if(clientPacket) {
            if(nbt.contains("Rotor")) {
                var posArray = nbt.getIntArray("Rotor");
                var pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
                var rotor = get(getWorld(), pos, RotorBehaviour.TYPE);
                if(rotor != null) {
                    this.rotor = rotor;
                } else {
                    PowerGrid.LOGGER.error("Client received rotor position which doesn't correspond to a rotor entity");
                }
            } else {
                rotor = null;
            }
        }
    }

    @Override
    public void write(NbtCompound nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        if(clientPacket && rotor != null) {
            var pos = rotor.getPos();
            nbt.putIntArray("Rotor", new int[] { pos.getX(), pos.getY(), pos.getZ() });
        }
    }

    public float emfVoltage() {
        if(rotor == null)
            return 0;
        return -rotor.getAngularVelocityRadians() * rotor.getFieldStrength() * coilConstant;
    }

    @Override
    public BehaviourType<CoilBehaviour> getType() {
        return TYPE;
    }

    @Override
    public void tick() {
        super.tick();

        if(rotor != null) {
            float current = coilEntity.windingCurrent();
            float torque = coilConstant * rotor.getFieldStrength() * current;

            float Pe = current * emfVoltage();
            if(Pe > 0) {
                // Generator is sourcing power
                // Apply more torque to account for losses, use
                // those losses to heat the coil up.
                torque *= 1.1f;
            } else {
                // Generator is sinking power
                // Reduce torque to account for losses
                torque *= 0.9f;
            }
            float Pm = rotor.getAngularVelocityRadians() * torque;
//            PowerGrid.LOGGER.info("Efficiency: {}", Pm / Pe);
//            PowerGrid.LOGGER.info("P_e: {}", current * emfVoltage());
//            PowerGrid.LOGGER.info("P_m: {}", rotor.getAngularVelocityRadians() * torque);

            rotor.applyTickForce(torque);
        }
    }
}
