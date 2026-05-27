package org.patryk3211.powergrid.compat.simulated.redstone;

import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlock;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import net.minecraft.core.Direction;
import org.patryk3211.powergrid.electricity.redstoneconverter.BlockEntityRedstoneConverterBehaviour;
import org.patryk3211.powergrid.mixin.simulated.GimbalSensorScrollMixin;

public class GimbalSensor extends BlockEntityRedstoneConverterBehaviour<GimbalSensorBlockEntity> {
    public GimbalSensor() {
        super(SimBlockEntityTypes.GIMBAL_SENSOR.get());
    }

    @Override
    public float getSignal(GimbalSensorBlockEntity be, Direction face) {
        var dir = face.getOpposite();
        var state = be.getBlockState();
        var axisBehaviour = (GimbalSensorScrollMixin) be.getBehaviour(GimbalSensorBlockEntity.GimbalSensorScrollValueBehaviour.TYPE);
        final boolean alongPrimary = (dir.getAxis() == state.getValue(GimbalSensorBlock.HORIZONTAL_AXIS));
        final float angleLimit = alongPrimary ? axisBehaviour.getPrimaryValue() : axisBehaviour.getSecondaryValue();

        double angle = face.getAxis() == Direction.Axis.Z ? be.getXAngle() : be.getZAngle();
        return (float) (angle / Math.toRadians(angleLimit));
    }
}
