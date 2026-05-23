package org.patryk3211.powergrid.compat.simulated.redstone;

import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.patryk3211.powergrid.electricity.redstoneconverter.BlockEntityRedstoneConverterBehaviour;

public class SteeringWheelSensor extends BlockEntityRedstoneConverterBehaviour<SteeringWheelBlockEntity> {
    public SteeringWheelSensor() {
        super(SimBlockEntityTypes.STEERING_WHEEL.get());
    }

    @Override
    public float getSignal(SteeringWheelBlockEntity be, Direction face) {
        face = face.getOpposite();
        Direction facing = be.getBlockState().getValue(SteeringWheelBlock.FACING);

        float frac = Mth.clamp(be.targetAngleToUpdate / be.angleInput.getValue(), -1, 1);

        if (facing == face) {
            return be.held ? 1 : 0;
        }

        if (Math.abs(be.getAngle()) < 0.99) {
            return 0;
        }
        frac *= ((facing.getStepX() == 1 || facing.getStepZ() == 1) ? -1 : 1);

        if (facing.getClockWise() == face && frac > 0) {
            return frac;
        } else if (facing.getCounterClockWise() == face && frac < 0) {
            return -frac;
        }
        return 0;
    }
}
