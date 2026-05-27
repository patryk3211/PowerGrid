package org.patryk3211.powergrid.compat.cold_sweat;

import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
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

        double temp = Math.max((behaviour.getTemperature() - ThermalBehaviour.STANDARD_TEMPERATURE) * ModdedConfigs.server().coldSweat.coldSweatTempScalar.get() / 100, 0);
        double rangeMax = Math.max((behaviour.getTemperature() - ThermalBehaviour.STANDARD_TEMPERATURE) * ModdedConfigs.server().coldSweat.coldSweatRangeScalar.get() / 100, 0);
        return CSMath.blend(temp, 0, distance, 0.5, rangeMax);
    }
}
