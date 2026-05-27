package org.patryk3211.powergrid.compat.simulated.redstone;

import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlock;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.patryk3211.powergrid.electricity.redstoneconverter.BlockEntityRedstoneConverterBehaviour;

public class SpringSensor extends BlockEntityRedstoneConverterBehaviour<TorsionSpringBlockEntity> {
    public SpringSensor() {
        super(SimBlockEntityTypes.TORSION_SPRING.get());
    }

    @Override
    public float getSignal(TorsionSpringBlockEntity be, Direction face) {
        Direction facing = be.getBlockState().getValue(TorsionSpringBlock.FACING);
        float frac = Mth.clamp(be.getAngle() / be.angleInput.getValue(), -1, 1);
        if (Math.abs(be.getAngle()) < 0.99) {
            return 0;
        }
        frac *= (facing.getStepX() == 1 || facing.getStepZ() == 1 ? -1 : 1);

        if (facing.getCounterClockWise() == face && frac > 0) {
            return frac;
        } else if (facing.getClockWise() == face && frac < 0) {
            return -frac;
        }
        return 0;
    }
}
