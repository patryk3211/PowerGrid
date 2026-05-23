package org.patryk3211.powergrid.compat.simulated;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.simulated_team.simulated.index.SimBlocks;
import org.patryk3211.powergrid.compat.simulated.redstone.AltitudeSensor;
import org.patryk3211.powergrid.compat.simulated.redstone.GimbalSensor;
import org.patryk3211.powergrid.compat.simulated.redstone.VelocitySensor;
import org.patryk3211.powergrid.electricity.redstoneconverter.RedstoneConverterRegistry;

public class SimulatedBridge {
    public static void init() {
        LifecycleEvent.SETUP.register(SimulatedBridge::setup);
    }

    public static void setup() {
        RedstoneConverterRegistry.REGISTRY.register(SimBlocks.ALTITUDE_SENSOR.get(), new AltitudeSensor());
        RedstoneConverterRegistry.REGISTRY.register(SimBlocks.GIMBAL_SENSOR.get(), new GimbalSensor());
        RedstoneConverterRegistry.REGISTRY.register(SimBlocks.VELOCITY_SENSOR.get(), new VelocitySensor());
    }
}
