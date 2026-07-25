package org.patryk3211.powergrid.compat.cc.gauges;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.gauge.GaugeBlockEntity;

public abstract class BaseGaugePeripheral implements IPeripheral {

    protected final GaugeBlockEntity gauge;

    protected BaseGaugePeripheral(GaugeBlockEntity gauge) {
        this.gauge = gauge;
    }

    @LuaFunction
    public double rangePercentage() {
        return gauge.getProgress();
    }

    @LuaFunction
    public float maxRange() {
        return gauge.getMaxValue();
    }

    @LuaFunction
    public float getValue() {
        return gauge.getValue();
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if (other instanceof BaseGaugePeripheral that) {
            return this.gauge == that.gauge;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return gauge.hashCode();
    }
}