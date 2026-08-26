package org.patryk3211.powergrid.electricity.febridge.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.febridge.FEInverterBlock;
import org.patryk3211.powergrid.electricity.febridge.FEInverterBlockEntity;

public class FEInverterBlockEntityImpl extends FEInverterBlockEntity {
    private final InverterFEStorage storage = new InverterFEStorage();

    public FEInverterBlockEntityImpl(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void useEnergy(int amount) {
        storage.energy -= amount;
        if(storage.energy < 0)
            storage.energy = 0;
        setChanged();
    }

    @Override
    protected int storedEnergy() {
        return storage.energy;
    }

    public @Nullable IEnergyStorage getEnergyStorage(Direction side) {
        if(side != getBlockState().getValue(FEInverterBlock.FACING))
            return null;
        return storage;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Energy", storage.energy);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        storage.energy = tag.getInt("Energy");
    }

    public class InverterFEStorage implements IEnergyStorage {
        public int energy;

        @Override
        public int receiveEnergy(int energy, boolean simulate) {
            int emptySpace = energyBufferSize() - this.energy;
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
