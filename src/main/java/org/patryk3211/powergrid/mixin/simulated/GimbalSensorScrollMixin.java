package org.patryk3211.powergrid.mixin.simulated;

import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GimbalSensorBlockEntity.GimbalSensorScrollValueBehaviour.class)
public interface GimbalSensorScrollMixin {
    @Accessor(remap = false)
    int getPrimaryValue();
    @Accessor(remap = false)
    int getSecondaryValue();
}
