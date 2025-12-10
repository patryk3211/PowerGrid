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
package org.patryk3211.powergrid.kinetics.punchcard;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchBlock;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;

import java.util.List;

public class PunchCardReaderBlockEntity extends ElectricKineticBlockEntity {
    private SwitchedWire[] wires;

    private LerpedFloat progress;
    private int oldIndex = -1;

    public PunchCardReaderBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour.reducedSync();
    }

    private float getChaseSpeed() {
        return Mth.clamp(Math.abs(getSpeed()) / 80 / 20, 0, 1);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        float speed = getSpeed();
        var facing = getBlockState().getValue(HvSwitchBlock.HORIZONTAL_FACING);
        if(facing == Direction.NORTH || facing == Direction.EAST)
            speed = -speed;
        progress.chase(speed > 0 ? 1 : 0, getChaseSpeed(), LerpedFloat.Chaser.LINEAR);
        sendData();
    }

    @Override
    public void tick() {
        super.tick();

        progress.tickChaser();
        var index = Math.round(progress.getValue() * 16);
        if(index != oldIndex) {
            for (int i = 0; i < 8; ++i) {
                wires[i].setState((index & (1 << i)) != 0);
            }
            oldIndex = index;
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(9);
        wires = new SwitchedWire[8];
        var common = builder.terminalNode(0);
        for(int i = 0; i < 8; ++i) {
            wires[i] = builder.connectSwitch(resistance(), common, builder.terminalNode(i + 1));
        }
    }
}
