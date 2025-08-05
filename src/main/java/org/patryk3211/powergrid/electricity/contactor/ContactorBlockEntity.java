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
package org.patryk3211.powergrid.electricity.contactor;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactorBlockEntity extends ElectricBlockEntity {
    private ElectricWire coil;

    private SwitchedWire switch1;
    private SwitchedWire switch2;

    private boolean state;
    private final Set<ContactorBlockEntity> external = new HashSet<>();

    public ContactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.forMaxPower(this, 2.0f, 2000f);
    }

    private void checkPos(BlockPos pos, boolean newState, List<BlockPos> checkQueue) {
        assert world != null;
        world.getBlockEntity(pos, ModdedBlockEntities.CONTACTOR.get())
                .ifPresent(be -> {
                    if(newState) {
                        be.addExternal(this);
                    } else {
                        be.removeExternal(this);
                    }
                    checkQueue.add(pos);
                });
    }

    private void setState(boolean newState) {
        if(newState || external.isEmpty()) {
            switch1.setState(newState);
            switch2.setState(newState);
        }
        if(state != newState && !world.isClient) {
            // Play sound
            if(newState) {
                ModdedSoundEvents.CONTACTOR_ON.playOnServer(world, pos);
            } else {
                ModdedSoundEvents.CONTACTOR_OFF.playOnServer(world, pos);
            }

            var checkQueue = new ArrayList<BlockPos>();
            checkQueue.add(pos);
            var checkedSet = new HashSet<BlockPos>();
            var axis = getCachedState().get(ContactorBlock.HORIZONTAL_AXIS);
            if(axis == Direction.Axis.X) {
                axis = Direction.Axis.Z;
            } else if(axis == Direction.Axis.Z) {
                axis = Direction.Axis.X;
            }

            while(!checkQueue.isEmpty()) {
                var checkPos = checkQueue.remove(0);
                if(!checkedSet.add(checkPos))
                    continue;
                var pos1 = checkPos.offset(axis, 1);
                checkPos(pos1, newState, checkQueue);
                var pos2 = checkPos.offset(axis, -1);
                checkPos(pos2, newState, checkQueue);
            }
        }
        state = newState;
    }

    private void addExternal(ContactorBlockEntity be) {
        external.add(be);
        if(!state) {
            switch1.setState(true);
            switch2.setState(true);
        }
    }

    private void removeExternal(ContactorBlockEntity be) {
        external.remove(be);
        if(external.isEmpty() && !state) {
            switch1.setState(false);
            switch2.setState(false);
        }
    }

    @Override
    public void tick() {
        super.tick();
        var I = Math.abs(coil.current());
        if (I > 2.0f) {
            setState(true);
        } else if (I < 1.9f) {
            setState(false);
        }

        applyLostPower(switch1.power());
        applyLostPower(switch2.power());
        applyLostPower(coil.power());
    }

    @Override
    public void remove() {
        super.remove();
        // Removed contactor as external holder of state
        setState(false);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(6);
        coil = builder.connect(ContactorBlock.coilResistance(), builder.terminalNode(0), builder.terminalNode(1));

        switch1 = builder.connectSwitch(ContactorBlock.switchResistance(), builder.terminalNode(2), builder.terminalNode(3), state);
        switch2 = builder.connectSwitch(ContactorBlock.switchResistance(), builder.terminalNode(4), builder.terminalNode(5), state);
    }
}
