package org.patryk3211.powergrid.electricity.redstoneconverter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class ContainerRedstoneConverterBehaviour implements IRedstoneConverterBehaviour {
    private static final IRedstoneConverterBehaviour CONTAINER_BE_IMPL = new ContainerRedstoneConverterBehaviour() {
        @Override
        public Container getContainer(Level level, BlockState state, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof Container container ? container : null;
        }
    };

    @Nullable
    public abstract Container getContainer(Level level, BlockState state, BlockPos pos);

    @Override
    public float getSignal(Level level, BlockState state, BlockPos pos, Direction face) {
        var container = getContainer(level, state, pos);
        if (container == null)
            return 0;
        float fill = 0;
        for (int j = 0; j < container.getContainerSize(); ++j) {
            var stack = container.getItem(j);
            if (!stack.isEmpty()) {
                fill += (float) stack.getCount() / Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
            }
        }

        return fill / container.getContainerSize();
    }

    public static IRedstoneConverterBehaviour chest(ChestBlock chestBlock) {
        return new ContainerRedstoneConverterBehaviour() {
            @Override
            public Container getContainer(Level level, BlockState state, BlockPos pos) {
                return ChestBlock.getContainer(chestBlock, state, level, pos, false);
            }
        };
    }

    public static IRedstoneConverterBehaviour containerBE() {
        return CONTAINER_BE_IMPL;
    }
}
