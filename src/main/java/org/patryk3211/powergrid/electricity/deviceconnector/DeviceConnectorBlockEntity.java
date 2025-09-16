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

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class DeviceConnectorBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    protected BridgeElectricBehaviour proxyBehaviour;
    protected SwitchedWire converterWire;

    public DeviceConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected BridgeElectricBehaviour makeBridge() {
        return new BridgeElectricBehaviour(this, worldPosition.relative(getBlockState().getValue(DeviceConnectorBlock.FACING)), () -> converterWire);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        proxyBehaviour = makeBridge();
        electricBehaviour = proxyBehaviour;
        behaviours.add(electricBehaviour);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        converterWire = builder.connectSwitch(100, builder.terminalNode(0), builder.terminalNode(1));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(!proxyBehaviour.isFE())
            return false;
        Lang.translate("gui.device_connector.info_header").forGoggles(tooltip);

        Lang.translate("gui.device_connector.supplying")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var unit = Lang.translate("gui.device_connector.supplying.unit");
        var A = Math.abs(converterWire.current());
        Lang.number(proxyBehaviour.currentRate)
                .add(Component.literal(" "))
                .add(unit)
                .style(ChatFormatting.AQUA)
                .add(Lang.translate("gui.device_connector.for")
                        .add(Lang.numberConstant(A * A * converterWire.getResistance()))
                        .add(Component.literal(" "))
                        .add(Unit.POWER.get())
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        Lang.translate("gui.device_connector.buffered")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var unit2 = Lang.translate("gui.device_connector.buffered.unit");
        var V = Math.abs(converterWire.potentialDifference());
        Lang.number(proxyBehaviour.getBufferedAmount())
                .add(Component.literal(" "))
                .add(unit2)
                .style(ChatFormatting.AQUA)
                .add(Lang.translate("gui.device_connector.for")
                        .add(Lang.numberConstant(V))
                        .add(Component.literal(" "))
                        .add(Unit.VOLTAGE.get())
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        return true;
    }
}
