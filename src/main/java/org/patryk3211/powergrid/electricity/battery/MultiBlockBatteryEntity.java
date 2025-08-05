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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
        assert world != null;
        if(isController()) {
            var r = world.random;
            var x = pos.getX() + r.nextBetween(0, getWidth());
            var y = pos.getY() + r.nextBetween(0, getHeight());
            var z = pos.getZ() + r.nextBetween(0, getWidth());
            ThermalBehaviour.explode(world, new BlockPos(x, y, z), getCachedState(), getSize() * 0.5f);
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

        thermalBehaviour = specifyThermalBehaviour()
                .behaviourFlags(ThermalBehaviour.OVERHEAT_PARTICLES)
                .overheatCallback(this::overheated);
        if(thermalBehaviour != null)
            behaviours.add(thermalBehaviour);
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (world.isClient)
            return;
        if (!isController())
            return;
        CustomConnectivityHandler.formMulti(this);
    }

    @Override
    public void updateBehaviour() {
        // This also finds wires connected through device connectors/proxy behaviours
        List<WireEntity> wires = null;
        if(world != null)
            wires = GlobalElectricNetworks.getWorldNetworks(world).findConnectedWires(electricBehaviour);
        if(isController()) {
            if(electricBehaviour instanceof ProxyElectricBehaviour proxy) {
                electricBehaviour = new ElectricBehaviour(this);
                electricBehaviour.inheritConnections(proxy);
                removeBehaviour(ElectricBehaviour.TYPE);
                attachBehaviourLate(electricBehaviour);
            }
            updateThermals();
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
            if(controller != null)
                thermalBehaviour.track(getControllerBE().thermalBehaviour);
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
            lastKnownPos = getPos();
        else if (!lastKnownPos.equals(pos) && pos != null) {
            onPositionChanged();
            return;
        }

        if(!isController()) {
            var controller = getControllerBE();
            if (controller != null)
                thermalBehaviour.track(getControllerBE().thermalBehaviour);
        }

        if (updateConnectivity)
            updateConnectivity();

        if(world.isClient && rewire) {
            updateBehaviour();
            rewire = false;
        }
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = pos;
    }

    @Override
    public BlockPos getController() {
        return isController() ? pos : controller;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MultiBlockBatteryEntity getControllerBE() {
        if (isController() || !hasWorld())
            return this;
        var blockEntity = world.getBlockEntity(controller);
        if (blockEntity instanceof MultiBlockBatteryEntity battery)
            return battery;
        return null;
    }

    @Override
    public boolean isController() {
        return controller == null || pos.getX() == controller.getX()
                && pos.getY() == controller.getY() && pos.getZ() == controller.getZ();
    }

    @Override
    public void setController(BlockPos controller) {
        if (world.isClient && !isVirtual())
            return;
        if (controller.equals(this.controller))
            return;
        this.controller = controller;
        updateBehaviour();
        notifyUpdate();
    }

    @Override
    public void removeController(boolean keepContents) {
        if (world.isClient)
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
    protected void read(NbtCompound compound, boolean clientPacket) {
        BlockPos controllerBefore = controller;
        int prevSize = width;
        int prevHeight = height;

        updateConnectivity = compound.contains("Uninitialized");
        controller = null;
        lastKnownPos = null;

        if (compound.contains("LastKnownPos"))
            lastKnownPos = NbtHelper.toBlockPos(compound.getCompound("LastKnownPos"));
        if (compound.contains("Controller"))
            controller = NbtHelper.toBlockPos(compound.getCompound("Controller"));

        if (isController()) {
            width = compound.getInt("Size");
            height = compound.getInt("Height");
        }
        var blocks = width * width * height;
        capacity = spec.getMaxCharge() * blocks;

        super.read(compound, clientPacket);

        boolean changeOfController = controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
        if(world != null && (changeOfController || prevSize != width || prevHeight != height)) {
            world.scheduleBlockRerenderIfNeeded(getPos(), Blocks.AIR.getDefaultState(), getCachedState());
            updateBehaviour();
        }

        if(clientPacket) {
            if(compound.getBoolean("Rewire"))
                rewire = true;
        }
    }

    @Override
    public void write(NbtCompound compound, boolean clientPacket) {
        if (updateConnectivity)
            compound.putBoolean("Uninitialized", true);
        if (lastKnownPos != null)
            compound.put("LastKnownPos", NbtHelper.fromBlockPos(lastKnownPos));
        if (!isController())
            compound.put("Controller", NbtHelper.fromBlockPos(controller));
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
        if(world.isClient) {
            // We defer the rewire because all block entities need to update their behaviours,
            // but after this invalidate we lose reference to nodes provided by the controller.
            var global = GlobalElectricNetworks.getWorldNetworks(world);
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
        thermalBehaviour.setDissipationFactor(spec.getDissipationFactor() * getSize());
        thermalBehaviour.setThermalMass(spec.getThermalMass() * getSize());
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
