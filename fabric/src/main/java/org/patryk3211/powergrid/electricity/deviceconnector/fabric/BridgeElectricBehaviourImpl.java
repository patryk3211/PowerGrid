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
package org.patryk3211.powergrid.electricity.deviceconnector.fabric;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.patryk3211.powergrid.electricity.deviceconnector.DeviceConnectorBlock;
import org.patryk3211.powergrid.electricity.febridge.IFEBridgeHandler;
import team.reborn.energy.api.EnergyStorage;

public class BridgeElectricBehaviourImpl {
    public static IFEBridgeHandler makeFEHandler(BlockEntity be) {
        var facing = be.getBlockState().getValue(DeviceConnectorBlock.FACING);
        var energyStorage = EnergyStorage.SIDED.find(be.getLevel(), be.getBlockPos().relative(facing), facing.getOpposite());
        if(energyStorage != null) {
            return new FEBridgeEnergyStorage(be);
        } else {
            return null;
        }
    }
}
