package org.patryk3211.powergrid.compat.cc.gauges;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.gauge.EnergyMeterBlockEntity;

public class EnergyMeterPeripheral implements IPeripheral {
    protected final EnergyMeterBlockEntity meter;

    public EnergyMeterPeripheral(EnergyMeterBlockEntity meter) {
        this.meter = meter;
    }

    @LuaFunction
    public float getValue() {
        return (float) meter.getEnergy();
    }

    @LuaFunction
    public float maxRange() {
        return 99999.9f;
    }

    @LuaFunction
    public float energy() {
        return (float) meter.getEnergy();
    }

    @Override
    public String getType() {
        return "powergrid_energy_meter";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if (other instanceof EnergyMeterPeripheral that) {
            return this.meter == that.meter;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return meter.hashCode();
    }
}
