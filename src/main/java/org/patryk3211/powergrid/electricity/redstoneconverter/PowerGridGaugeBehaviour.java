package org.patryk3211.powergrid.electricity.redstoneconverter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.gauge.GaugeBlockEntity;

public class PowerGridGaugeBehaviour implements IRedstoneConverterBehaviour {
    @Override
    public float getSignal(Level level, BlockState state, BlockPos pos, Direction face) {
        var be = level.getBlockEntity(pos);
        if(be instanceof GaugeBlockEntity gauge) {
            return gauge.dialTarget;
        }
        return 0;
    }
}
