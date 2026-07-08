package org.patryk3211.powergrid.electricity.sim.special;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.calculation.Precalculated;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.G_MIN;

public class GeneratorCoupling extends VoltageSourceCoupling implements IOuterHook {
    private final IRotor rotor;
    private float field;
    private float baseResistance;
    private double backEmf;
    private double emfValue;

    private Precalculated<Float> fieldStrength;

    public GeneratorCoupling(IElectricNode positive, @Nullable IElectricNode negative, Number resistance, IRotor rotor) {
        super(positive, negative, resistance);
        this.rotor = rotor;
        baseResistance = resistance.floatValue();
        if(baseResistance <= 0)
            baseResistance = (float) (1 / G_MIN);
    }

    public void setField(float field) {
        this.field = field;
        double dt = network == null ? 0.05 : network.getDeltaTime();
        if(rotor.getInertia() != 0) {
            backEmf = field * field * dt / rotor.getInertia();
        } else {
            backEmf = 0;
        }
        super.setResistance((float) (baseResistance + backEmf));
    }

    public void setEmfValue(double value) {
        this.emfValue = value;
    }

    public double getEmfValue() {
        return emfValue;
    }

    @Override
    public void addStaticResidual(IResidualAdder residual) {
        super.addStaticResidual(residual);
        if(isConverged())
            emfValue = -backEmf * getCurrent();
        // Only apply if it will raise the output voltage,
        // this should result in an inductor like behaviour,
        // which will negate the backEmf resistance.
        if(Math.signum(emfValue) == Math.signum(getVoltage()))
            residual.add(index, emfValue);
    }

    @Override
    public void setResistance(float resistance) {
        baseResistance = resistance;
        if(baseResistance <= 0)
            baseResistance = (float) (1 / G_MIN);
        super.setResistance((float) (resistance + backEmf));
    }

    public void setFieldStrengthProvider(Precalculated<Float> fieldStrength) {
        this.fieldStrength = fieldStrength;
    }

    @Override
    public void preSolve() {
        setField(fieldStrength.get());
        setVoltage(field * rotor.getAngularVelocityRadians());
    }

    @Override
    public void postUpperSolve() {
        if(isConverged()) {
            rotor.applyTickForce((float) (field * getCurrent() / network.getMultiTick()));
        }
    }
}
