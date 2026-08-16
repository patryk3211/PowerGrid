package org.patryk3211.powergrid.compat.cc.battery;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.patryk3211.powergrid.electricity.battery.MultiBlockBatteryEntity;

public class BatteryPeripheral implements IPeripheral {
    private final MultiBlockBatteryEntity battery;

    public BatteryPeripheral(MultiBlockBatteryEntity battery) {
        this.battery = battery.getControllerBE();
    }

    @Override
    public @NonNull String getType() {
        return "powergrid_battery";
    }

    @LuaFunction
    public double capacity() {
        return battery.getCapacity();
    }

    @LuaFunction
    public double energy() {
        return battery.getEnergy();
    }

    @LuaFunction
    public int chargePercentage() {
        return battery.getCurrentValue();
    }

    @LuaFunction
    public float powerDraw() {
        return battery.calculatePower();
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if(other instanceof BatteryPeripheral that) {
            return this.battery  == that.battery;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return battery.hashCode();
    }
}
