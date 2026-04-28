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

import com.drmangotea.tfmg.content.electricity.base.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.deviceconnector.BridgeElectricBehaviour;
import org.patryk3211.powergrid.electricity.deviceconnector.DeviceConnectorBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.DeviceConnectorBlockEntity;

public class TFMGCompatDeviceConnectorBlockEntity extends DeviceConnectorBlockEntity implements IElectric {
    public ElectricBlockValues data = new ElectricBlockValues(getPos());
    private int voltage;
    private boolean powerRefresh = false;
    private boolean firstUpdate = false;

    public TFMGCompatDeviceConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        data.connectNextTick = true;
    }

    @Override
    protected BridgeElectricBehaviour makeBridge() {
        return new TFMGBridgeElectricBehaviour(this,
                worldPosition.relative(getBlockState().getValue(DeviceConnectorBlock.FACING)), () -> converterWire);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        var newVoltage = (int) Math.abs(converterWire.potentialDifference());
        if (newVoltage != voltage) {
            if (firstUpdate) {
                firstUpdate = false;
            } else {
                voltage = newVoltage;
                updateNetwork();
                refreshPower();
            }
        }
        lazyTickElectricity();
    }

    private void refreshPower() {
        powerRefresh = false;
        float power = getGeneratorLoad();
        var resistance = voltage * voltage / power;
        if (power > 0 && resistance > 0) {
            converterWire.setResistance(resistance);
        } else {
            converterWire.setResistance(1e+6);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (powerRefresh)
            refreshPower();
        tickElectricity();
    }

    @Override
    public long getPos() {
        return getBlockPos().asLong();
    }

    @Override
    public LevelAccessor getLevelAccessor() {
        return level;
    }

    @Override
    public ElectricBlockValues getData() {
        return data;
    }

    @Override
    public float resistance() {
        return 0;
    }

    @Override
    public int voltageGeneration() {
        return voltage;
    }

    @Override
    public int powerGeneration() {
        return ModdedConfigs.server().electricity.tfmgConnectorPower.get();
    }

    @Override
    public int getNetworkResistance() {
        return data.networkResistance;
    }

    @Override
    public void updateNextTick() {
        data.updateNextTick = true;
    }

    @Override
    public void onNetworkChanged(int oldVoltage, int oldPower) {
        powerRefresh = true;
    }

    @Override
    public void updateNetwork() {
        getOrCreateElectricNetwork().updateNetwork();
        if (!level.isClientSide)
            PacketDistributor.sendToAllPlayers(new NetworkUpdatePacket(BlockPos.of(getPos())));
        sendData();
    }

    @Override
    public void sendStuff() {
        sendData();
    }

    @Override
    public void setVoltage(int i) {
        data.voltage = i;
    }

    @Override
    public void setNetwork(long network) {
        this.data.electricalNetworkId = network;
        if (network != getPos())
            ElectricNetworkManager.networks.get(getLevel())
                    .remove(getPos());
    }

    @Override
    public void remove() {
        super.remove();
        onRemoved();
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        readElectricity(compound, clientPacket);
    }

    @Override
    public boolean hasElectricitySlot(Direction direction) {
        return getBlockState().getValue(DeviceConnectorBlock.FACING) == direction;
    }
}
