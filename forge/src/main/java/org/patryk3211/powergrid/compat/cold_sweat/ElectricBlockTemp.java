package org.patryk3211.powergrid.compat.cold_sweat;

import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

public class ElectricBlockTemp extends BlockTemp {
    @Override
    public boolean hasBlock(Block block) {
        return block instanceof ElectricBlock; // Should get rid of the large majority of blocks
    }

    @Override
    public double getTemperature(Level level, @Nullable LivingEntity entity, BlockState state, BlockPos pos, double distance) {
        var behaviour = BlockEntityBehaviour.get(level, pos, ThermalBehaviour.TYPE);
        if (behaviour == null)
            return 0;

        // 1200ºC will approximately equal to a seething blaze burner
        double temp = Math.max(Temperature.convert(behaviour.getTemperature() - 22f, Temperature.Units.C, Temperature.Units.MC, false) / 80, 0);
        double rangeMax = temp * 11.67; // around 7 blocks at 1200ºC
        return CSMath.blend(temp, 0, distance, 0.5, rangeMax);
    }
}