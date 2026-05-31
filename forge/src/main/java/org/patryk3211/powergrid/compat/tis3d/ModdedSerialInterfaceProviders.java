package org.patryk3211.powergrid.compat.tis3d;

import com.tterrag.registrate.util.entry.RegistryEntry;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.gauge.CurrentGaugeBlock;
import org.patryk3211.powergrid.electricity.gauge.CurrentGaugeBlockEntity;
import org.patryk3211.powergrid.electricity.gauge.PowerGaugeBlock;
import org.patryk3211.powergrid.electricity.gauge.PowerGaugeBlockEntity;
import org.patryk3211.powergrid.electricity.gauge.VoltageGaugeBlock;
import org.patryk3211.powergrid.electricity.gauge.VoltageGaugeBlockEntity;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedSerialInterfaceProviders {
    public static final RegistryEntry<SerialInterfaceProvider, GaugeBlockSerialProvider<VoltageGaugeBlock, VoltageGaugeBlockEntity>> VOLTAGE_METER =
            REGISTRATE.simple(
                    "voltage_meter",
                    SerialInterfaceProvider.REGISTRY,
                    () -> GaugeBlockSerialProvider.of(ModdedBlocks.VOLTAGE_METER)
            );

    public static final RegistryEntry<SerialInterfaceProvider, GaugeBlockSerialProvider<CurrentGaugeBlock, CurrentGaugeBlockEntity>> CURRENT_METER =
            REGISTRATE.simple(
                    "current_meter",
                    SerialInterfaceProvider.REGISTRY,
                    () -> GaugeBlockSerialProvider.of(ModdedBlocks.CURRENT_METER)
            );

    public static final RegistryEntry<SerialInterfaceProvider, GaugeBlockSerialProvider<PowerGaugeBlock, PowerGaugeBlockEntity>> POWER_METER =
            REGISTRATE.simple(
                    "power_meter",
                    SerialInterfaceProvider.REGISTRY,
                    () -> GaugeBlockSerialProvider.of(ModdedBlocks.POWER_METER)
            );

    public static void register() { /* Initialize static fields. */ }
}
