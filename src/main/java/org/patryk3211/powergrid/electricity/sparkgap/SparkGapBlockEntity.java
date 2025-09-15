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
package org.patryk3211.powergrid.electricity.sparkgap;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.particles.ZapParticleData;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class SparkGapBlockEntity extends ElectricBlockEntity {
    private SwitchedWire plasmaChannel;

    protected SparkGapValueBehaviour setting;

    public SparkGapBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        setting = new SparkGapValueBehaviour(Lang.translateDirect("devices.spark_gap.voltage"), this, new BoxTransform());
        behaviours.add(setting);
    }

    @Override
    public void tick() {
        super.tick();
        if(level.isClientSide && plasmaChannel.getState()) {
            var center = worldPosition.getCenter().subtract(0, 0.125f, 0);
            float offset = (1 + setting.getValue() * 3 / 18f) / 16f;

            var axis = getBlockState().getValue(SparkGapBlock.HORIZONTAL_AXIS);
            var end = center.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE), offset);
            var start = center.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE), offset);
            level.addParticle(new ZapParticleData(end.x, end.y, end.z, true)
                            .withLife(1)
                            .withSegments(5),
                    start.x, start.y, start.z, 0, 0, 0);
        }

        if(!plasmaChannel.getState() && Math.abs(plasmaChannel.potentialDifference()) > setting.getVoltage()) {
            plasmaChannel.setState(true);
            notifyUpdate();
        } else if(plasmaChannel.getState() && Math.abs(plasmaChannel.current()) < setting.getCurrent()) {
            plasmaChannel.setState(false);
            notifyUpdate();
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        plasmaChannel.setState(tag.getBoolean("State"));
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("State", plasmaChannel.getState());
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        plasmaChannel = builder.connectSwitch(1, builder.terminalNode(0), builder.terminalNode(1), false);
    }

    public static class BoxTransform extends CenteredSideValueBoxTransform {
        public BoxTransform() {
            super((state, dir) -> dir == Direction.UP);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0f, 8.0f, 8.5f);
        }
    }
}
