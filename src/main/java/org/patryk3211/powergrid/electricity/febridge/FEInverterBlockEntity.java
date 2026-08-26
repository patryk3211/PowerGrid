package org.patryk3211.powergrid.electricity.febridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.ProvidedVoltageSourceCoupling;
import org.patryk3211.powergrid.electricity.sim.special.CRSeriesWire;

public class FEInverterBlockEntity extends ElectricBlockEntity {
    private ProvidedVoltageSourceCoupling outputSource;
    private CRSeriesWire control;
    private float prevThrottling;

    public FEInverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private float rawThrottling() {
        return Mth.clamp((float) (control.capacitorVoltage() / ModdedConfigs.server().electricity.feInverterControlVoltage.get()), 0, 1);
    }

    protected float inputThrottling() {
        return Mth.clamp((rawThrottling() + prevThrottling) * 0.5f, 0, 1);
    }

    public static int energyBufferSize() {
        return ModdedConfigs.server().electricity.feInverterBufferSize.get();
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
        prevThrottling = rawThrottling() * 0.5f + prevThrottling * 0.5f;
        double power = -outputSource.getCurrent() * outputSource.getVoltage();
        power -= outputSource.getCurrent() * outputSource.getCurrent() * outputSource.getResistance();
        setUnsaved();
        if(power < 0)
            return;
        int fe = (int) Math.ceil(ModdedConfigs.server().electricity.forgeEnergyPerWatt.getF() * power);
        useEnergy(fe);
    }

    protected float outputVoltage() {
        return (storedEnergy() / ModdedConfigs.server().electricity.forgeEnergyPerVolt.getF() * (1 - inputThrottling()));
    }

    protected float outputResistance() {
        float V = outputVoltage();
        float W = storedEnergy() / ModdedConfigs.server().electricity.forgeEnergyPerWatt.getF();
        float R = V * V / (2 * W);
        if(W <= 0 || R <= 0)
            return 1000;
        return R;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("ControlVoltage", (float) control.capacitorVoltage());
        tag.putFloat("PrevThrottle", prevThrottling);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        control.setVoltage(tag.getFloat("ControlVoltage"));
        prevThrottling = tag.getFloat("PrevThrottle");
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(3);
        outputSource = new ProvidedVoltageSourceCoupling(builder.terminalNode(0), builder.terminalNode(1), 1);
        outputSource.setVoltageProvider(this::outputVoltage);
        outputSource.setResistanceProvider(this::outputResistance);
        builder.add(outputSource);
        control = new CRSeriesWire(
                ModdedConfigs.server().electricity.feInverterControlCapacitance.get(),
                10000, builder.terminalNode(2), builder.terminalNode(1));
        builder.connect(100000, builder.terminalNode(2), builder.terminalNode(1));
        builder.add(control);
    }

    protected void useEnergy(int amount) {
        throw new AssertionError("Unimplemented");
    }

    protected int storedEnergy() {
        throw new AssertionError("Unimplemented");
    }
}
