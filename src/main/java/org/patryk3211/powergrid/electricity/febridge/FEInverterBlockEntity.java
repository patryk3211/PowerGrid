package org.patryk3211.powergrid.electricity.febridge;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.ProvidedVoltageSourceCoupling;

public class FEInverterBlockEntity extends ElectricBlockEntity {
    private ProvidedVoltageSourceCoupling outputSource;
    private ElectricWire control;
    private float prevThrottling;

    public FEInverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private float rawThrottling() {
        return Mth.clamp((float) (control.potentialDifference() / 5f), 0, 1);
    }

    protected float inputThrottling() {
        return (rawThrottling() + prevThrottling) * 0.5f;
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
        prevThrottling = rawThrottling();
        double power = -outputSource.getCurrent() * outputSource.getVoltage();
        power -= outputSource.getCurrent() * outputSource.getCurrent() * outputSource.getResistance();
        if(power < 0)
            return;
        int fe = (int) (ModdedConfigs.server().electricity.forgeEnergyPerWatt.getF() * power);
        useEnergy(fe);
    }

    protected float outputVoltage() {
        return Math.min(storedEnergy() / ModdedConfigs.server().electricity.forgeEnergyPerVolt.getF(), ModdedConfigs.server().electricity.feInverterMaxVoltage.get());
    }

    protected float outputResistance() {
        float V = outputVoltage();
        float W = storedEnergy() / ModdedConfigs.server().electricity.forgeEnergyPerWatt.getF();
        float R = V * V / W;
        if(W <= 0 || R <= 0)
            return 1000;
        return R;
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(3);
        outputSource = new ProvidedVoltageSourceCoupling(builder.terminalNode(0), builder.terminalNode(1), resistance());
        outputSource.setVoltageProvider(this::outputVoltage);
        outputSource.setResistanceProvider(this::outputResistance);
        builder.add(outputSource);
        control = builder.connect(1000, builder.terminalNode(2), builder.terminalNode(1));
    }

    protected void useEnergy(int amount) {
        throw new AssertionError("Unimplemented");
    }

    protected int storedEnergy() {
        throw new AssertionError("Unimplemented");
    }
}
