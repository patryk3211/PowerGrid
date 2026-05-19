package org.patryk3211.powergrid.electricity.redstoneconverter;

import com.simibubi.create.content.kinetics.gauge.GaugeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CreateGaugeBehaviour implements IRedstoneConverterBehaviour {
    @Override
    public float getSignal(Level level, BlockState state, BlockPos pos, Direction face) {
        var be = level.getBlockEntity(pos);
        if(be instanceof GaugeBlockEntity gauge) {
            return gauge.dialTarget;
        }
        return 0;
    }
}
