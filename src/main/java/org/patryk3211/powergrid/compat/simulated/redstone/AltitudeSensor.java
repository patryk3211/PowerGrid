package org.patryk3211.powergrid.compat.simulated.redstone;

import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import net.minecraft.core.Direction;
import org.patryk3211.powergrid.electricity.redstoneconverter.BlockEntityRedstoneConverterBehaviour;

public class AltitudeSensor extends BlockEntityRedstoneConverterBehaviour<AltitudeSensorBlockEntity> {
    public AltitudeSensor() {
        super(SimBlockEntityTypes.ALTITUDE_SENSOR.get());
    }

    @Override
    public float getSignal(AltitudeSensorBlockEntity be, Direction face) {
        return be.getValue();
    }
}
