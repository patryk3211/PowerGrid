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
package org.patryk3211.powergrid.electricity.battery;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.List;

public class BatteryBlock extends AbstractBatteryBlock<MultiBlockBatteryEntity> implements IAcceptConnector {
    protected BatterySpec spec;

    public BatteryBlock(Settings settings) {
        super(settings);
    }

    public static <T extends BatteryBlock, P> NonNullUnaryOperator<BlockBuilder<T, P>> setSpec(BatterySpec spec) {
        return b -> b.onRegister(block -> block.spec = spec);
    }

    public BatterySpec getSpec() {
        return spec;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved) {
        if(oldState.getBlock() == state.getBlock())
            return;
        if(moved)
            return;
        // fabric: see comment in FluidTankItem
//        Consumer<FluidTankBlockEntity> consumer = FluidTankItem.IS_PLACING_NBT
//                ? FluidTankBlockEntity::queueConnectivityUpdate
//                : FluidTankBlockEntity::updateConnectivity;
        withBlockEntityDo(world, pos, MultiBlockBatteryEntity::updateConnectivity);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
            var be = world.getBlockEntity(pos);
            if (!(be instanceof MultiBlockBatteryEntity battery))
                return;
            var wires = GlobalElectricNetworks.getWorldNetworks(world).findConnectedWires(battery.getElectricBehaviour());
            super.onStateReplaced(state, world, pos, newState, moved);
            CustomConnectivityHandler.splitMulti(battery);

            // Rewire all wires that still target the stale behaviour
            // TODO: Rewire must also happen on the client
            wires.forEach(WireEntity::dropWire);
            wires.forEach(WireEntity::makeWire);
        }
    }

    @Override
    public Class<MultiBlockBatteryEntity> getBlockEntityClass() {
        return MultiBlockBatteryEntity.class;
    }

    @Override
    public BlockEntityType<? extends MultiBlockBatteryEntity> getBlockEntityType() {
        return ModdedBlockEntities.MULTIBLOCK_BATTERY.get();
    }

    @Override
    public boolean isPolarized() {
        return true;
    }
}
