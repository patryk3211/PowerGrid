package org.patryk3211.powergrid.electricity.redstoneconverter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DaylightSensorBehaviour implements IRedstoneConverterBehaviour {
    @Override
    public float getSignal(Level level, BlockState state, BlockPos pos, Direction face) {
        float levelLight = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
        float sunAngle = level.getSunAngle(1.0F);
        boolean invert = state.getValue(DaylightDetectorBlock.INVERTED);
        if (invert) {
            levelLight = 15 - levelLight;
        } else if (levelLight > 0) {
            float g = sunAngle < (float)Math.PI ? 0.0F : ((float)Math.PI * 2F);
            sunAngle += (g - sunAngle) * 0.2F;
            levelLight = levelLight * Mth.cos(sunAngle);
        }

        return Mth.clamp(levelLight / 15f, 0, 1);
    }
}
