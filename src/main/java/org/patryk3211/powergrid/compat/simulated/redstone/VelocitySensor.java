package org.patryk3211.powergrid.compat.simulated.redstone;

import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlock;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import net.minecraft.core.Direction;
import org.patryk3211.powergrid.electricity.redstoneconverter.BlockEntityRedstoneConverterBehaviour;

public class VelocitySensor extends BlockEntityRedstoneConverterBehaviour<VelocitySensorBlockEntity> {
    public VelocitySensor() {
        super(SimBlockEntityTypes.VELOCITY_SENSOR.get());
    }

    @Override
    public float getSignal(VelocitySensorBlockEntity be, Direction face) {
        final int powered = be.getBlockState().getValue(VelocitySensorBlock.POWERED);
        if (powered == 0) {
            return 0;
        }
        Direction positiveDir = VelocitySensorBlock.getDirectionOfAxis(be.getBlockState());
        if (powered == 2) {
            positiveDir = positiveDir.getOpposite();
        }

        if (face == positiveDir) {
            return 0;
        }
        return Math.abs(be.getAdjustedVelocity() / be.getMaxSpeed().getValue());
    }
}
