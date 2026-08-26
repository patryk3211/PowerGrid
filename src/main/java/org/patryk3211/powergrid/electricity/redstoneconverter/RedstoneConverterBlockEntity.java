package org.patryk3211.powergrid.electricity.redstoneconverter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

public class RedstoneConverterBlockEntity extends ElectricBlockEntity {
    private ElectricWire inverting, nonInverting;

    public Float signalOverride = null;

    public RedstoneConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void electricalTick() {
        applyPower(inverting);
        applyPower(nonInverting);
        var state = getBlockState();
        var facing = state.getValue(RedstoneConverterBlock.FACING);
        var pos = worldPosition.relative(facing);
        float signal;
        if(signalOverride != null) {
            signal = signalOverride;
        } else {
            signal = RedstoneConverterRegistry.get(level, level.getBlockState(pos), pos, facing.getOpposite());
        }
        if(!Float.isFinite(signal))
            signal = 0;
        signal = Mth.clamp(signal, 0, 1);
        if(signalOverride == null) {
            float signal2 = level.getDirectSignalTo(worldPosition) / 15.0f;
            signal = Math.max(signal, signal2);
        }
        if (signal > 1 / 30f && !state.getValue(RedstoneConverterBlock.POWERED)) {
            level.setBlockAndUpdate(worldPosition, state.setValue(RedstoneConverterBlock.POWERED, true));
        } else if (signal < 1 / 30f && state.getValue(RedstoneConverterBlock.POWERED)) {
            level.setBlockAndUpdate(worldPosition, state.setValue(RedstoneConverterBlock.POWERED, false));
        }
        updateResistance(signal);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(3);
        nonInverting = builder.connect(resistance("min"), builder.terminalNode(0), builder.terminalNode(2));
        inverting = builder.connect(resistance("max"), builder.terminalNode(2), builder.terminalNode(1));
    }

    public void updateResistance(float strength) {
        strength = Mth.clamp(strength, 0, 1);
        float min = resistance("min"), max = resistance("max");
        nonInverting.setResistance(min * strength + max * (1 - strength));
        inverting.setResistance(min * (1 - strength) + max * strength);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if(tag.contains("SignalOverride")) {
            signalOverride = tag.getFloat("SignalOverride");
        } else {
            signalOverride = null;
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if(signalOverride != null) {
            tag.putFloat("SignalOverride", signalOverride);
        }
    }
}
