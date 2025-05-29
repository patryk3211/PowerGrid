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
package org.patryk3211.powergrid.electricity.electricswitch;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class SwitchBlockEntity extends ElectricBlockEntity {
    private SwitchedWire wire;
    private float maxVoltage;
    private boolean switchState = false;
    private Float overvoltResistance = null;

    public SwitchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return new ThermalBehaviour(this, 0.5f, 0.1f);
    }

    @Override
    public void tick() {
        super.tick();
        var current = wire.current();
        applyLostPower(current * current * wire.getResistance());
        if(wire.potentialDifference() > maxVoltage && overvoltResistance == null) {
            wire.setState(true);
            // Pick a random resistance for failed switches to spice things up.
            overvoltResistance = world.random.nextFloat() * 1000f;
            wire.setResistance(overvoltResistance);
        }
    }

    public void setState(boolean state) {
        switchState = state;
        if(overvoltResistance == null)
            wire.setState(state);
        notifyUpdate();
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if(clientPacket) {
            switchState = tag.getBoolean("State");
            wire.setState(switchState);
        }
        if(tag.contains("Overvolted")) {
            overvoltResistance = tag.getFloat("Overvolted");
        }
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(clientPacket) {
            tag.putBoolean("State", switchState);
        }
        if(overvoltResistance != null) {
            tag.putFloat("Overvolted", overvoltResistance);
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        var node1 = builder.addExternalNode();
        var node2 = builder.addExternalNode();

        if(!(getCachedState().getBlock() instanceof SwitchBlock block))
            throw new IllegalArgumentException("Blocks with SwitchBlockEntity must inherit from SwitchBlock");
        maxVoltage = block.getMaxVoltage();
        wire = builder.connectSwitch(block.getResistance(), node1, node2, !getCachedState().get(SwitchBlock.OPEN));
        if(overvoltResistance != null) {
            wire.setResistance(overvoltResistance);
            wire.setState(true);
        }
    }
}
