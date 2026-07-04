package org.patryk3211.powergrid.electricity.febridge.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.febridge.FEInverterBlock;
import org.patryk3211.powergrid.electricity.febridge.FEInverterBlockEntity;

public class FEInverterBlockEntityImpl extends FEInverterBlockEntity {
    private final LazyOptional<InverterFEStorage> storage = LazyOptional.of(InverterFEStorage::new);

    public FEInverterBlockEntityImpl(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static int energyBufferSize() {
        return (int) (ModdedConfigs.server().electricity.forgeEnergyPerVolt.getF() * ModdedConfigs.server().electricity.feInverterMaxVoltage.get() * 10);
    }

    @Override
    protected void useEnergy(int amount) {
        storage.ifPresent(handler -> {
            handler.energy -= amount;
            if(handler.energy < 0)
                handler.energy = 0;
            setChanged();
        });
    }

    @Override
    protected int storedEnergy() {
        return storage.map(handler -> handler.energy).orElse(0);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ENERGY && side == getBlockState().getValue(FEInverterBlock.FACING))
            return storage.cast();
        return super.getCapability(cap, side);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        storage.ifPresent(handler -> tag.putInt("Energy", handler.energy));
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        storage.ifPresent(handler -> handler.energy = tag.getInt("Energy"));
    }

    public class InverterFEStorage implements IEnergyStorage {
        public int energy;

        @Override
        public int receiveEnergy(int energy, boolean simulate) {
            int emptySpace = energyBufferSize() - this.energy;
//            if(!simulate)
                energy *= 1 - inputThrottling();
            int received = Math.min(energy, emptySpace);
            if(!simulate) {
                this.energy += received;
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int energy, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return energyBufferSize();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
