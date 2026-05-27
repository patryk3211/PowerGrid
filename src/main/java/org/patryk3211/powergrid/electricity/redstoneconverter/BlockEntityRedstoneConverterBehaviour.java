package org.patryk3211.powergrid.electricity.redstoneconverter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BlockEntityRedstoneConverterBehaviour<T extends BlockEntity> implements IRedstoneConverterBehaviour {
    private final BlockEntityType<T> type;

    public BlockEntityRedstoneConverterBehaviour(BlockEntityType<T> type) {
        this.type = type;
    }

    public abstract float getSignal(T be, Direction face);

    @Override
    public float getSignal(Level level, BlockState state, BlockPos pos, Direction face) {
        return level.getBlockEntity(pos, type)
                .map(be -> getSignal(be, face))
                .orElse(0.0f);
    }
}
