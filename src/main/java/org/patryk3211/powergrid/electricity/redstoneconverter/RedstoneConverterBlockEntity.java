package org.patryk3211.powergrid.electricity.redstoneconverter;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

public class RedstoneConverterBlockEntity extends ElectricBlockEntity {
    private ElectricWire inverting, nonInverting;

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
        float signal = RedstoneConverterRegistry.get(level, level.getBlockState(pos), pos, facing.getOpposite());
        if(!Float.isFinite(signal))
            signal = 0;
        signal = Mth.clamp(signal, 0, 1);
        float signal2 = level.getDirectSignalTo(worldPosition) / 15.0f;
        signal = Math.max(signal, signal2);
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
}
