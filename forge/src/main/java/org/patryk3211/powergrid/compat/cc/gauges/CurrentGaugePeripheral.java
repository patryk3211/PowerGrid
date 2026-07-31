package org.patryk3211.powergrid.compat.cc.gauges;

import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.gauge.GaugeBlockEntity;

public class CurrentGaugePeripheral extends BaseGaugePeripheral {
    public CurrentGaugePeripheral(GaugeBlockEntity gauge) {
        super(gauge);
    }

    @Override
    public @NotNull String getType() {
        return "powergrid_current_gauge";
    }

    @LuaFunction
    public double current() {
        return getValue();
    }

    public static CurrentGaugePeripheral of(BlockEntity be) {
        return new CurrentGaugePeripheral((GaugeBlockEntity) be);
    }
}