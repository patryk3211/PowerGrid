package org.patryk3211.powergrid.electricity.sim.special;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

public class GeneratorCoupling extends VoltageSourceCoupling {
    private final IRotor rotor;
    private float field;
    private float baseResistance;

    public GeneratorCoupling(IElectricNode positive, @Nullable IElectricNode negative, Number resistance, IRotor rotor) {
        super(positive, negative, resistance);
        this.rotor = rotor;
        baseResistance = resistance.floatValue();
    }

    public void setField(float field) {
        this.field = field;
        var backEmf = field / rotor.getInertia() * 0.05f;
        super.setResistance(baseResistance + backEmf);
    }

    @Override
    public void setResistance(float resistance) {
        this.baseResistance = resistance;
        var backEmf = field / rotor.getInertia() * 0.05f;
        super.setResistance(resistance + backEmf);
    }

    public void tick(float newField) {
        rotor.applyTickForce(field * getCurrent());

        setField(newField);
        setVoltage(field * rotor.getAngularVelocityRadians());
    }
}
