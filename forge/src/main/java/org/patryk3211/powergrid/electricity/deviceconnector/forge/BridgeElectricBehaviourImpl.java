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
package org.patryk3211.powergrid.electricity.deviceconnector.forge;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.patryk3211.powergrid.electricity.deviceconnector.DeviceConnectorBlock;
import org.patryk3211.powergrid.electricity.febridge.IFEBridgeHandler;

public class BridgeElectricBehaviourImpl {
    public static IFEBridgeHandler makeFEHandler(BlockEntity be) {
        var facing = be.getBlockState().getValue(DeviceConnectorBlock.FACING);
        var neighbor = be.getLevel().getBlockEntity(be.getBlockPos().relative(facing));
        var capability = neighbor.getCapability(ForgeCapabilities.ENERGY, facing.getOpposite());
        if(capability.filter(IEnergyStorage::canReceive).isEmpty())
            return null;
        return new FEBridgeEnergyStorage(be);
    }
}
