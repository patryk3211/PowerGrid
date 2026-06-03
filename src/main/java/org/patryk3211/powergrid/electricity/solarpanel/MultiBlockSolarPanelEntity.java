package org.patryk3211.powergrid.electricity.solarpanel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ProxyElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;

import java.util.List;

public class MultiBlockSolarPanelEntity extends SolarPanelBlockEntity implements IMultiBlockSolarPanel {
    protected int width = 1;
    protected int height = 1;
    protected BlockPos lastKnownPos;
    protected BlockPos controller;
    private boolean rewire;
    protected boolean updateConnectivity;
    private int connectedBlocks = 1;

    //TODO Doesn't work atm

    public MultiBlockSolarPanelEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level.isClientSide && !isVirtual())
            return;
        if (!isController())
            return;
        CustomSolarConnectivityHandler.formMulti(this);
    }

    @Override
    public void tick() {
        if (isController()) {
            super.tick();
        }
    }

    public void queueConnectivityUpdate() {
        updateConnectivity = true;
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
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

//    @Override
//    protected void read(CompoundTag compound, boolean clientPacket) {
//        BlockPos controllerBefore = controller;
//        int prevSize = width;
//        int prevHeight = height;
//
//        updateConnectivity = compound.contains("Uninitialized");
//        controller = null;
//        lastKnownPos = null;
//
//        if (compound.contains("LastKnownPos"))
//            lastKnownPos = NbtUtils.readBlockPos(compound.getCompound("LastKnownPos"));
//        if (compound.contains("Controller"))
//            controller = NbtUtils.readBlockPos(compound.getCompound("Controller"));
//
//        if (isController()) {
//            width = compound.getInt("Size");
//            height = compound.getInt("Height");
//        }
//        totalCells = CELLS_IN_SERIES * getSize();
//
//        super.read(compound, clientPacket);
//
//        boolean changeOfController = controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
//        if(level != null && (changeOfController || prevSize != width || prevHeight != height)) {
//            level.setBlocksDirty(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState());
//            updateBehaviour();
//        }
//
//        if(clientPacket) {
//            if(compound.getBoolean("Rewire"))
//                rewire = true;
//        }
//    }
//
//    @Override
//    public void write(CompoundTag compound, boolean clientPacket) {
//        if (updateConnectivity)
//            compound.putBoolean("Uninitialized", true);
//        if (lastKnownPos != null)
//            compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
//        if (!isController())
//            compound.put("Controller", NbtUtils.writeBlockPos(controller));
//        if (isController()) {
//            compound.putInt("Size", width);
//            compound.putInt("Height", height);
//        }
//        super.write(compound, clientPacket);
//
//        if(clientPacket) {
//            if(rewire) {
//                compound.putBoolean("Rewire", true);
//                rewire = false;
//            }
//        }
//    }

    @Override
    @SuppressWarnings("unchecked")
    public MultiBlockSolarPanelEntity getControllerBE() {
        if (isController() || !hasLevel())
            return this;
        var blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof MultiBlockSolarPanelEntity panel)
            return panel;
        return null;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX()
                && worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
    }

    @Override
    public void notifyMultiUpdated() {
        if(isController()) {
            totalCells = CELLS_IN_SERIES * getSize();
            updateThermals();
        }
        if(!level.isClientSide)
            notifyUpdate();
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    private void updateThermals() {
        if(thermalBehaviour != null) {
            var block = getBlockState().getBlock();
            var factor = ThermalBehaviour.dissipationFactor(ThermalValues.getPower(block), 175f);
            thermalBehaviour.setDissipationFactor(factor * getSize());
            thermalBehaviour.setThermalMass(ThermalValues.getMass(block) * getSize());
        }
    }

    public void markRewire() {
        rewire = true;
        sendData();
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
        controller = null;
        width = 1;
        height = 1;
        updateBehaviour();
        notifyUpdate();
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return Direction.Axis.X;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        if (longAxis == Direction.Axis.Z)
            return 3;
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
    public int getSize() {
        return Math.max(width * width, 1);
    }


    @Override
    public void updateBehaviour() {
        // This also finds wires connected through device connectors/proxy behaviours
        List<TransmissionLinePart> wires = null;
        if(level != null)
            wires = GlobalElectricNetworks.getWorldNetworks(level).findConnectedWires(electricBehaviour);
        if(isController()) {
            if(electricBehaviour instanceof ProxyElectricBehaviour proxy) {
                electricBehaviour = new ElectricBehaviour(this);
                electricBehaviour.inheritConnections(proxy);
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
                old.pause();
                attachBehaviourLate(electricBehaviour);
                sourceCoupling = null;
                System.out.println("sourceCoupling set null");
            }
            var controller = getControllerBE();
            if(controller != null && thermalBehaviour != null)
                thermalBehaviour.track(controller.thermalBehaviour);
        }
        if(wires != null) {
            // Rewire connected wires.
            wires.forEach(TransmissionLinePart::refreshEndpointNodes);
        }
    }

    @Override
    public void setCapacitySize(int blocks) {
        connectedBlocks = blocks;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }
}
