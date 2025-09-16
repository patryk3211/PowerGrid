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
package org.patryk3211.powergrid.compat.tfmg;

import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.deviceconnector.BridgeElectricBehaviour;
import org.patryk3211.powergrid.electricity.deviceconnector.DeviceConnectorBlock;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import java.util.function.Supplier;

public class TFMGBridgeElectricBehaviour extends BridgeElectricBehaviour {
    public <T extends SmartBlockEntity & IElectricEntity> TFMGBridgeElectricBehaviour(T be, BlockPos behaviourPosition, Supplier<SwitchedWire> converterWire) {
        super(be, behaviourPosition, converterWire);
    }

    @Override
    protected void constructBehaviours() {
        var world = getWorld();
        if(!world.isLoaded(behaviourPosition.get()))
            return;
        var be = world.getBlockEntity(behaviourPosition.get());
        if(be instanceof IElectric electric) {
            if(!electric.hasElectricitySlot(blockEntity.getBlockState().getValue(DeviceConnectorBlock.FACING).getOpposite())) {
                world.destroyBlock(getPos(), true);
            }
            return;
        }
        super.constructBehaviours();
    }
}
