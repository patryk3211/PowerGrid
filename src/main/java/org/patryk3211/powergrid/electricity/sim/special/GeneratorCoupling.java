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

    private double V;

    public GeneratorCoupling(IElectricNode positive, @Nullable IElectricNode negative, Number resistance, IRotor rotor) {
        super(positive, negative, resistance);
        this.rotor = rotor;
        baseResistance = resistance.floatValue();
    }

    public void setField(float field) {
        this.field = field;
        backEmf = field * field * DT / rotor.getInertia(); //2 * (5) / 0.05f;//field * field * DT / rotor.getInertia();
        super.setResistance(baseResistance + backEmf);
    }

    @Override
    public void addStaticResidual(IResidualAdder residual) {
        super.addStaticResidual(residual);

        var V_Inductor = (getCurrent() * backEmf + V);// * backEmf / (backEmf + baseResistance);
//        var G_I = 1 / backEmf;

//        var residualScale = 1 - G_I / (1 / baseResistance + G_I);
        V = -backEmf * getCurrent();// - V_Inductor;// - V_Inductor) * 1.00001f;
        // We only want this to oppose, not make the output stronger
//        if(Math.signum(V) != Math.signum(getVoltage()))
//            V *= 0.5f;
        residual.add(index, V);
    }

    @Override
    public void setResistance(float resistance) {
        this.baseResistance = resistance;
        super.setResistance(resistance + backEmf);
    }

    public void tick(float newField) {
        if(isConverged())
            rotor.applyTickForce(field * getCurrent());

        setField(newField);
        setVoltage(field * rotor.getAngularVelocityRadians());
    }
}
