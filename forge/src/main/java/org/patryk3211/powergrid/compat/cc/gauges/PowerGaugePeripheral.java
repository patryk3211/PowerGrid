package org.patryk3211.powergrid.compat.cc.gauges;

import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;
import org.patryk3211.powergrid.electricity.gauge.GaugeBlockEntity;

public class PowerGaugePeripheral extends BaseGaugePeripheral {
    public PowerGaugePeripheral(GaugeBlockEntity gauge) {
        super(gauge);
    }

    @Override
    public @NonNull String getType() {
        return "powergrid_power_gauge";
    }

    @LuaFunction
    public double power() {
        return getValue();
    }

    public static PowerGaugePeripheral of(BlockEntity be) {
        return new PowerGaugePeripheral((GaugeBlockEntity) be);
    }
}