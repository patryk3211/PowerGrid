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

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ProxyElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.List;

public class MultiBlockBatteryEntity extends BatteryBlockEntity implements IMultiBlockBattery {
    protected int width = 1;
    protected int height = 1;
    protected boolean updateConnectivity;
    private boolean rewire;

    protected BlockPos lastKnownPos;
    protected BlockPos controller;

    public MultiBlockBatteryEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        updateBehaviour();
        sendData();
    }

    @Override
    public void updateParameters() {
        if(energy <= 0 || sourceNode == null)
            return;
        float chargeLevel = (float) (energy / capacity);
        sourceNode.setVoltage(spec.calculateVoltage(chargeLevel));
        coupling.setResistance(spec.calculateResistance(chargeLevel) / getSize());
    }

    private void overheated() {
        assert level != null;
        if(isController()) {
            var r = level.random;
            var x = worldPosition.getX() + r.nextIntBetweenInclusive(0, getWidth());
            var y = worldPosition.getY() + r.nextIntBetweenInclusive(0, getHeight());
            var z = worldPosition.getZ() + r.nextIntBetweenInclusive(0, getWidth());
            ThermalBehaviour.explode(level, new BlockPos(x, y, z), getBlockState(), getSize() * 0.5f);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if(isController()) {
            electricBehaviour = new ElectricBehaviour(this);
        } else {
            electricBehaviour = new ProxyElectricBehaviour(this, this::getController);
        }
        behaviours.add(electricBehaviour);

        thermalBehaviour = specifyThermalBehaviour();
        if(thermalBehaviour != null) {
            thermalBehaviour.behaviourFlags(ThermalBehaviour.OVERHEAT_PARTICLES)
                    .overheatCallback(this::overheated);
            behaviours.add(thermalBehaviour);
        }
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level.isClientSide && !isVirtual())
            return;
        if (!isController())
            return;
        CustomConnectivityHandler.formMulti(this);
    }

    @Override
    public void updateBehaviour() {
        // This also finds wires connected through device connectors/proxy behaviours
        List<WireEntity> wires = null;
        if(level != null)
            wires = GlobalElectricNetworks.getWorldNetworks(level).findConnectedWires(electricBehaviour);
        if(isController()) {
            if(electricBehaviour instanceof ProxyElectricBehaviour proxy) {
                electricBehaviour = new ElectricBehaviour(this);
                electricBehaviour.inheritConnections(proxy);
                removeBehaviour(ElectricBehaviour.TYPE);
                attachBehaviourLate(electricBehaviour);
            }
            updateThermals();
            if(thermalBehaviour != null)
                thermalBehaviour.track(null);
        } else {
            if(!(electricBehaviour instanceof ProxyElectricBehaviour)) {
                var old = electricBehaviour;
                electricBehaviour = new ProxyElectricBehaviour(this, this::getController);
                electricBehaviour.inheritConnections(old);
                removeBehaviour(ElectricBehaviour.TYPE);
                attachBehaviourLate(electricBehaviour);
                sourceNode = null;
                coupling = null;
            }
            var controller = getControllerBE();
            if(controller != null && thermalBehaviour != null)
                thermalBehaviour.track(controller.thermalBehaviour);
        }
        if(wires != null) {
            // Rewire connected wires.
            wires.forEach(WireEntity::dropWire);
            wires.forEach(WireEntity::makeWire);
        }
        updateParameters();
    }

    public void markRewire() {
        rewire = true;
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (lastKnownPos == null)
            lastKnownPos = getBlockPos();
        else if (!lastKnownPos.equals(worldPosition)) {
            onPositionChanged();
            return;
        }

        if(!isController()) {
            var controller = getControllerBE();
            if (controller != null && thermalBehaviour != null)
                thermalBehaviour.track(getControllerBE().thermalBehaviour);
        }

        if (updateConnectivity)
            updateConnectivity();

        if(level.isClientSide && rewire) {
            updateBehaviour();
            rewire = false;
        }
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MultiBlockBatteryEntity getControllerBE() {
        if (isController() || !hasLevel())
            return this;
        var blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof MultiBlockBatteryEntity battery)
            return battery;
        return null;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX()
                && worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
    }

    @Override
    public void setController(BlockPos controller) {
        if (level.isClientSide && !isVirtual())
            return;
        if (controller.equals(this.controller))
            return;
        this.controller = controller;
        updateBehaviour();
        notifyUpdate();
    }

    @Override
    public void removeController(boolean keepContents) {
        if (level.isClientSide && !isVirtual())
            return;
        updateConnectivity = true;
        if (!keepContents) {
            // Split charge between all batteries
            var currentBlocks = spec.getMaxCharge() / capacity;
            capacity = spec.getMaxCharge();
            energy /= currentBlocks;
        }
        controller = null;
        width = 1;
        height = 1;
        updateBehaviour();
        notifyUpdate();
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        BlockPos controllerBefore = controller;
        int prevSize = width;
        int prevHeight = height;

        updateConnectivity = compound.contains("Uninitialized");
        controller = null;
        lastKnownPos = null;

        if (compound.contains("LastKnownPos"))
            lastKnownPos = NbtUtils.readBlockPos(compound.getCompound("LastKnownPos"));
        if (compound.contains("Controller"))
            controller = NbtUtils.readBlockPos(compound.getCompound("Controller"));

        if (isController()) {
            width = compound.getInt("Size");
            height = compound.getInt("Height");
        }
        var blocks = width * width * height;
        capacity = spec.getMaxCharge() * blocks;

        super.read(compound, clientPacket);

        boolean changeOfController = controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
        if(level != null && (changeOfController || prevSize != width || prevHeight != height)) {
            level.setBlocksDirty(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState());
            updateBehaviour();
        }

        if(clientPacket) {
            if(compound.getBoolean("Rewire"))
                rewire = true;
        }
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        if (updateConnectivity)
            compound.putBoolean("Uninitialized", true);
        if (lastKnownPos != null)
            compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        if (!isController())
            compound.put("Controller", NbtUtils.writeBlockPos(controller));
        if (isController()) {
            compound.putInt("Size", width);
            compound.putInt("Height", height);
        }
        super.write(compound, clientPacket);

        if(clientPacket) {
            if(rewire) {
                compound.putBoolean("Rewire", true);
                rewire = false;
            }
        }
    }

    public double getIndividualEnergy() {
        var controller = getControllerBE();
        if(controller == null)
            return getEnergy();
        return controller.getEnergy() / controller.getSize();
    }

    @Override
    public void invalidate() {
        if(level.isClientSide) {
            // We defer the rewire because all block entities need to update their behaviours,
            // but after this invalidate we lose reference to nodes provided by the controller.
            var global = GlobalElectricNetworks.getWorldNetworks(level);
            var wires = global.findConnectedWires(electricBehaviour);
            global.deferredRewire(wires);
        }
        super.invalidate();
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    public void queueConnectivityUpdate() {
        updateConnectivity = true;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        if(isController()) {
            capacity = spec.getMaxCharge() * getSize();
            updateParameters();
        }
        notifyUpdate();
    }

    private void updateThermals() {
        if(thermalBehaviour != null) {
            var block = getBlockState().getBlock();
            var factor = ThermalBehaviour.dissipationFactor(ThermalValues.getPower(block), 175f);
            thermalBehaviour.setDissipationFactor(factor * getSize());
            thermalBehaviour.setThermalMass(ThermalValues.getMass(block) * getSize());
        }
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return Direction.Axis.Y;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        if (longAxis == Direction.Axis.Y)
            return 5;
        return getMaxWidth();
    }

    @Override
    public int getMaxWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public BatterySpec getSpec() {
        return spec;
    }

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public void inheritEnergy(IMultiBlockBattery part) {
        energy += part.getEnergy();
        part.setEnergy(0);
    }

    @Override
    public void setCapacitySize(int blocks) {
        capacity = spec.getMaxCharge() * blocks;
    }

    @Override
    public int getSize() {
        return Math.max(width * width * height, 1);
    }
}
