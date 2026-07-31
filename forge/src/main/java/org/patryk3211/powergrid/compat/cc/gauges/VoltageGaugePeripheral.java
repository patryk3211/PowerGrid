package org.patryk3211.powergrid.compat.cc.gauges;

import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.gauge.GaugeBlockEntity;

public class VoltageGaugePeripheral extends BaseGaugePeripheral {
    public VoltageGaugePeripheral(GaugeBlockEntity gauge) {
        super(gauge);
    }

    @Override
    public @NotNull String getType() {
        return "powergrid_voltage_gauge";
    }

    @LuaFunction
    public double voltage() {
        return getValue();
    }

    public static VoltageGaugePeripheral of(BlockEntity be) {
        return new VoltageGaugePeripheral((GaugeBlockEntity) be);
    }
}