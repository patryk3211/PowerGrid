package org.patryk3211.powergrid.compat.cc;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.compat.cc.clutch.GeneratorClutchPeripheral;
import org.patryk3211.powergrid.compat.cc.gauges.CurrentGaugePeripheral;
import org.patryk3211.powergrid.compat.cc.gauges.EnergyMeterPeripheral;
import org.patryk3211.powergrid.compat.cc.gauges.PowerGaugePeripheral;
import org.patryk3211.powergrid.compat.cc.gauges.VoltageGaugePeripheral;
import org.patryk3211.powergrid.electricity.gauge.EnergyMeterBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.clutch.GeneratorClutchBlockEntity;

public class CCBridge {
    public static void register() {
        ForgeComputerCraftAPI.registerPeripheralProvider((level, pos, side) -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return LazyOptional.empty();

            var type = be.getType();
            if (type == ModdedBlockEntities.VOLTAGE_METER.get()) {
                return LazyOptional.of(() -> VoltageGaugePeripheral.of(be));
            } else if (type == ModdedBlockEntities.CURRENT_METER.get()) {
                return LazyOptional.of(() -> CurrentGaugePeripheral.of(be));
            } else if (type == ModdedBlockEntities.POWER_METER.get()) {
                return LazyOptional.of(() -> PowerGaugePeripheral.of(be));
            } else if (type == ModdedBlockEntities.GENERATOR_CLUTCH.get()) {
                return LazyOptional.of(() -> new GeneratorClutchPeripheral((GeneratorClutchBlockEntity) be));
            } else if (type == ModdedBlockEntities.ENERGY_METER.get()) {
                return LazyOptional.of(() -> new EnergyMeterPeripheral((EnergyMeterBlockEntity) be));
            }
            return LazyOptional.empty();
        });
    }
}