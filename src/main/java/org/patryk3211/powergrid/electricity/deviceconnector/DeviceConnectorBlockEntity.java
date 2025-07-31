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
package org.patryk3211.powergrid.electricity.deviceconnector;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;

public class DeviceConnectorBlockEntity extends ElectricBlockEntity {
    private BridgeElectricBehaviour proxyBehaviour;
    private SwitchedWire converterWire;

    public DeviceConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        var state = getCachedState();
        proxyBehaviour = new BridgeElectricBehaviour(this, pos.offset(state.get(DeviceConnectorBlock.FACING)), () -> converterWire);
        electricBehaviour = proxyBehaviour;
        behaviours.add(electricBehaviour);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        converterWire = builder.connectSwitch(100, builder.terminalNode(0), builder.terminalNode(1));
    }

    @Nullable
    public EnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return proxyBehaviour.getBridgeBehaviour();
    }
}
