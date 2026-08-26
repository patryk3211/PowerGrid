package org.patryk3211.powergrid.electricity.sim.node;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;

import java.util.function.Supplier;

public class ProvidedVoltageSourceCoupling extends VoltageSourceCoupling implements IOuterHook {
    private Supplier<? extends Number> voltageProvider = null;
    private Supplier<? extends Number> resistanceProvider = null;

    public ProvidedVoltageSourceCoupling(IElectricNode positive, @Nullable IElectricNode negative, float resistance) {
        super(positive, negative, resistance);
    }

    public ProvidedVoltageSourceCoupling(IElectricNode positive, @Nullable IElectricNode negative, Number resistance) {
        super(positive, negative, resistance);
    }

    public ProvidedVoltageSourceCoupling(IElectricNode positive, @Nullable IElectricNode negative, float resistance, float voltage) {
        super(positive, negative, resistance, voltage);
    }

    @Override
    public void preSolve() {
        if(voltageProvider != null)
            setVoltage(voltageProvider.get().doubleValue());
        if(resistanceProvider != null)
            setResistance(resistanceProvider.get().floatValue());
    }

    public void setVoltageProvider(Supplier<? extends Number> provider) {
        this.voltageProvider = provider;
    }

    public void setResistanceProvider(Supplier<? extends Number> provider) {
        this.resistanceProvider = provider;
    }
}
