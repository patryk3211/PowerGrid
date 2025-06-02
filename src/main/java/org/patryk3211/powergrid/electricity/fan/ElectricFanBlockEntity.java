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
package org.patryk3211.powergrid.electricity.fan;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

import javax.annotation.Nullable;
import java.util.List;

import static com.simibubi.create.content.kinetics.base.KineticBlockEntity.convertToDirection;
import static net.minecraft.state.property.Properties.FACING;

/**
 * @see com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity
 */
public class ElectricFanBlockEntity extends ElectricBlockEntity implements IAirCurrentSource {
    public AirCurrent airCurrent;
    protected int airCurrentUpdateCooldown;
    protected int entitySearchCooldown;
    protected boolean updateAirFlow;

    private ElectricWire motor;
    private float prevSpeed;

    public ElectricFanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        airCurrent = new AirCurrent(this);
        updateAirFlow = true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        registerAwardables(behaviours, AllAdvancements.FAN_PROCESSING);
    }

    @Override
    protected void read(NbtCompound compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if (clientPacket)
            airCurrent.rebuild();
    }

    @Override
    public void write(NbtCompound compound, boolean clientPacket) {
        super.write(compound, clientPacket);
    }

    @Override
    public AirCurrent getAirCurrent() {
        return airCurrent;
    }

    @Nullable
    @Override
    public World getAirCurrentWorld() {
        return world;
    }

    @Override
    public BlockPos getAirCurrentPos() {
        return pos;
    }

    @Override
    public float getSpeed() {
        return motor.current() * 64f;
    }

    @Override
    public Direction getAirflowOriginSide() {
        return this.getCachedState().get(FACING);
    }

    @Override
    public Direction getAirFlowDirection() {
        float speed = getSpeed();
        if(speed == 0)
            return null;
        Direction facing = getCachedState().get(FACING);
        speed = convertToDirection(speed, facing);
        return speed > 0 ? facing : facing.getOpposite();
    }

    @Override
    public void remove() {
        super.remove();
        updateChute();
    }

    @Override
    public boolean isSourceRemoved() {
        return removed;
    }

    public void onSpeedChanged() {
        updateAirFlow = true;
        updateChute();
    }

    public void updateChute() {
        Direction direction = getCachedState().get(FACING);
        if(!direction.getAxis().isVertical())
            return;
        BlockEntity poweredChute = world.getBlockEntity(pos.offset(direction));
        if(!(poweredChute instanceof ChuteBlockEntity))
            return;
        ChuteBlockEntity chuteBE = (ChuteBlockEntity) poweredChute;
        if(direction == Direction.DOWN)
            chuteBE.updatePull();
        else
            chuteBE.updatePush(1);
    }

    public void blockInFrontChanged() {
        updateAirFlow = true;
    }

    @Override
    public void tick() {
        super.tick();

        boolean server = !world.isClient || isVirtual();
        var speed = getSpeed();
        if(speed != prevSpeed) {
            onSpeedChanged();
            prevSpeed = speed;
        }

        if(server && airCurrentUpdateCooldown-- <= 0) {
            airCurrentUpdateCooldown = AllConfigs.server().kinetics.fanBlockCheckRate.get();
            updateAirFlow = true;
        }

        if(updateAirFlow) {
            updateAirFlow = false;
            airCurrent.rebuild();
//            if(airCurrent.maxDistance > 0)
//                award(AllAdvancements.ENCASED_FAN);
            sendData();
        }

        if(getSpeed() == 0)
            return;

        if(entitySearchCooldown-- <= 0) {
            entitySearchCooldown = 5;
            airCurrent.findEntities();
        }

        airCurrent.tick();
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        motor = builder.connect(20f, builder.terminalNode(0), builder.terminalNode(1));
    }
}
