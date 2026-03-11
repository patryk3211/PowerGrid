package org.patryk3211.powergrid.electricity.sim.special;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;

public class GeneratorCoupling extends VoltageSourceCoupling {
    private static final float DT = 0.05f;

    private final IRotor rotor;
    private float field;
    private float baseResistance;
    private float backEmf;

    public GeneratorCoupling(IElectricNode positive, @Nullable IElectricNode negative, Number resistance, IRotor rotor) {
        super(positive, negative, resistance);
        this.rotor = rotor;
        baseResistance = resistance.floatValue();
    }

    public void setField(float field) {
        this.field = field;
        backEmf = field * field * DT / rotor.getInertia();
        super.setResistance(baseResistance + backEmf);
    }

    @Override
    public void addStaticResidual(IResidualAdder residual) {
        super.addStaticResidual(residual);
        var v = -backEmf * getCurrent();
        // Only apply if it will raise the output voltage,
        // this should result in an inductor like behaviour,
        // which will negate the backEmf resistance.
        if(Math.signum(v) == Math.signum(getVoltage()))
            residual.add(index, v);
    }

    @Override
    public void setResistance(float resistance) {
        this.baseResistance = resistance;
        super.setResistance(resistance + backEmf);
    }

    public void tick(float newField) {
        if(isConverged())
            rotor.applyTickForce((float) (field * getCurrent()));

        setField(newField);
        setVoltage(field * rotor.getAngularVelocityRadians());
    }
}
