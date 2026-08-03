package org.patryk3211.powergrid.compat.cc;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.compat.cc.clutch.GeneratorClutchPeripheral;
import org.patryk3211.powergrid.compat.cc.gauges.CurrentGaugePeripheral;
import org.patryk3211.powergrid.compat.cc.gauges.PowerGaugePeripheral;
import org.patryk3211.powergrid.compat.cc.gauges.VoltageGaugePeripheral;

public class CCBridge {
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                PeripheralCapability.get(),
                ModdedBlockEntities.VOLTAGE_METER.get(),
                (be, direction) -> VoltageGaugePeripheral.of(be)
        );
        event.registerBlockEntity(
                PeripheralCapability.get(),
                ModdedBlockEntities.CURRENT_METER.get(),
                (be, direction) -> CurrentGaugePeripheral.of(be)
        );
        event.registerBlockEntity(
                PeripheralCapability.get(),
                ModdedBlockEntities.POWER_METER.get(),
                (be, direction) -> PowerGaugePeripheral.of(be)
        );
        event.registerBlockEntity(
                PeripheralCapability.get(),
                ModdedBlockEntities.GENERATOR_CLUTCH.get(),
                (be, direction) -> new GeneratorClutchPeripheral(be)
        );
    }
}