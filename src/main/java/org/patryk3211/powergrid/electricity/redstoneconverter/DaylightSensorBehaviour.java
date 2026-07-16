package org.patryk3211.powergrid.electricity.redstoneconverter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DaylightSensorBehaviour implements IRedstoneConverterBehaviour {
    @Override
    public float getSignal(Level level, BlockState state, BlockPos pos, Direction face) {
        // 0.25 - 0.75 = Night
        float timeOfDay = level.getTimeOfDay(1.0f);
        boolean invert = state.getValue(DaylightDetectorBlock.INVERTED);
        if (invert) {
            timeOfDay -= 0.25f;
            return 1 - Math.abs(timeOfDay - 0.25f) / 0.25f;
        } else {
            if(timeOfDay > 0.75f) {
                timeOfDay -= 0.75f;
            } else {
                timeOfDay += 0.25f;
            }
            return 1 - Math.abs(timeOfDay - 0.25f) / 0.25f;
        }
    }
}
