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

import com.drmangotea.tfmg.TFMG;
import com.drmangotea.tfmg.content.electricity.base.*;
import com.drmangotea.tfmg.registry.TFMGPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
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
        data.group = new ElectricalGroup(-1);
    }

    @Override
    protected BridgeElectricBehaviour makeBridge() {
        return new TFMGBridgeElectricBehaviour(this, worldPosition.relative(getBlockState().getValue(DeviceConnectorBlock.FACING)), () -> converterWire);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        var newVoltage = (int) Math.abs(converterWire.potentialDifference());
        if(newVoltage != voltage) {
            if(firstUpdate) {
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
        if(power > 0 && resistance > 0) {
            converterWire.setResistance(resistance);
            converterWire.setState(true);
        } else {
            converterWire.setState(false);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(powerRefresh)
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
    public int frequencyGeneration() {
        return 0;
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
            TFMGPackets.getChannel().send(PacketDistributor.ALL.noArg(), new NetworkUpdatePacket(BlockPos.of(getPos())));
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
    public void setFrequency(int i) {
        data.frequency = i;
    }

    @Override
    public void setNetworkResistance(int i) {
        data.networkResistance = i;
    }

    @Override
    public void setNetwork(long network) {
        this.data.electricalNetworkId = network;
        if (network != getPos())
            ElectricNetworkManager.networks.get(getLevel())
                    .remove(getPos());
    }

    @Override
    public boolean destroyed() {
        return data.destroyed;
    }

    @Override
    public void remove() {
        super.remove();
        this.data.destroyed = true;
        for (Direction d : Direction.values()) {
            if (hasElectricitySlot(d))
                if (getLevelAccessor().getBlockEntity(BlockPos.of(getPos()).relative(d)) instanceof IElectric be && be.hasElectricitySlot(d.getOpposite())) {
                    ElectricNetworkManager.networks.get(getLevel())
                            .remove(be.getPos());
                    be.setNetwork(be.getPos());
                    be.onPlaced();
                    be.updateNextTick();
                }
        }
        if (data.electricalNetworkId != getPos())
            getOrCreateElectricNetwork().getMembers().remove(this);

        if (data.electricalNetworkId == getPos())
            ElectricNetworkManager.networks.get(getLevel())
                    .remove(getData().getId());
    }

    @Override
    public ElectricalNetwork getOrCreateElectricNetwork() {
        if (level.getBlockEntity(BlockPos.of(data.electricalNetworkId)) instanceof IElectric) {
            return TFMG.NETWORK_MANAGER.getOrCreateNetworkFor((IElectric) level.getBlockEntity(BlockPos.of(data.electricalNetworkId)));
        } else {
            ElectricNetworkManager.networks.get(getLevel()).remove(data.electricalNetworkId);
            return TFMG.NETWORK_MANAGER.getOrCreateNetworkFor(this);
        }
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("GroupId", data.group.id);
        compound.putFloat("GroupResistance", data.group.resistance);
        compound.putInt("PrevVoltage", voltage);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        data.group = new ElectricalGroup(compound.getInt("GroupId"));
        data.group.resistance = compound.getFloat("GroupResistance");
        if (!clientPacket) {
            data.connectNextTick = true;
            voltage = compound.getInt("PrevVoltage");
        }
    }

    @Override
    public boolean hasElectricitySlot(Direction direction) {
        return getBlockState().getValue(DeviceConnectorBlock.FACING) == direction;
    }
}
