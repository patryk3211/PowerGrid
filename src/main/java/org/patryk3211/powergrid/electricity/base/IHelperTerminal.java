package org.patryk3211.powergrid.electricity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IHelperTerminal {
    @Nullable
    ITerminalPlacement connectedTerminal(BlockState state, int index);

    @NotNull
    default BlockPos connectedTerminalPosition(BlockPos pos, BlockState state, int index) {
        return pos;
    }
}
