package org.patryk3211.powergrid.compat.cc.clutch;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.patryk3211.powergrid.kinetics.generator.clutch.GeneratorClutchBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;

public class GeneratorClutchPeripheral implements IPeripheral {
    private final GeneratorClutchBlockEntity clutch;

    public GeneratorClutchPeripheral(GeneratorClutchBlockEntity clutch) {
        this.clutch = clutch;
    }

    @Override
    public @NonNull String getType() {
        return "powergrid_generator_clutch";
    }

    @LuaFunction
    public String mode() {
        var clutchMode = clutch.getBehaviour(ScrollOptionBehaviour.TYPE);
        if (!(clutchMode instanceof ScrollOptionBehaviour)) {
            return "UNKNOW";
        }
        return ((ScrollOptionBehaviour<?>) clutchMode).get().toString();
    }

    @LuaFunction
    public double load() {
        return clutch.getLoad();
    }

    @LuaFunction
    public double rpm() {
        var rotorBehaviour = clutch.getBehaviour(RotorBehaviour.TYPE);
        if (rotorBehaviour == null) {
            return 0;
        }
        return rotorBehaviour.getAngularVelocity();
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if (other instanceof GeneratorClutchPeripheral that) {
            return this.clutch == that.clutch;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return clutch.hashCode();
    }

}