package org.patryk3211.powergrid.electricity.redstoneconverter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class DefaultRedstoneConverterBehaviour implements IRedstoneConverterBehaviour {
    public static final DefaultRedstoneConverterBehaviour INSTANCE = new DefaultRedstoneConverterBehaviour();

    private DefaultRedstoneConverterBehaviour() {

    }

    @Override
    public float getSignal(Level level, BlockState state, BlockPos pos, Direction face) {
        if(state.hasAnalogOutputSignal()) {
            return state.getAnalogOutputSignal(level, pos) / 15.0f;
        } else if(state.isSignalSource()) {
            return state.getSignal(level, pos, face) / 15.0f;
        } else {
            return level.getSignal(pos, face) / 15.0f;
        }
    }
}
