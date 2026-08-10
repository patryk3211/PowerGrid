package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedAdvancements;
import org.patryk3211.powergrid.collections.ModdedMenus;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

import java.util.List;

public class EnergyMeterBlockEntity extends ElectricBlockEntity implements MenuProvider {
    private ElectricWire series;
    private ElectricWire shunt;

    double lastEnergy;
    double energy;

    private int lastRedstoneEnergy;
    private int redstoneTick;
    private int impulses;
    boolean measurementPrecision;

    public EnergyMeterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        registerAwardables(behaviours, ModdedAdvancements.ENERGY_METER_ROLLOVER);
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
        if(++redstoneTick >= 2) {
            if(this.impulses != 0) {
                // Guarantee that the comparator output is always a pulse
                this.impulses = 0;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            } else {
                int impulses = (int) energy - lastRedstoneEnergy;
                if (impulses < 0) impulses = 0;
                if (impulses > 15) impulses = 15;
                if (this.impulses != impulses) {
                    this.impulses = impulses;
                    level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                }
                lastRedstoneEnergy = (int) energy;
            }
            redstoneTick = 0;
        }
        setUnsaved();
    }

    @Override
    public void tick() {
        applyPower(series);
        lastEnergy = energy;
        energy += series.current() * shunt.potentialDifference() * 0.05 / (measurementPrecision ? 3_600 : 3_600_000);
        if(energy < 0) {
            lastEnergy += 100000;
            energy = 100000 + energy;
        }
        if(energy > 100000) {
            lastEnergy -= 100000;
            energy -= 100000;
            award(ModdedAdvancements.ENERGY_METER_ROLLOVER);
        }
        super.tick();
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    public int pulses() {
        return impulses;
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(3);
        series = builder.connect(resistance("series"), builder.terminalNode(0), builder.terminalNode(1));
        shunt = builder.connect(resistance("shunt"), builder.terminalNode(0), builder.terminalNode(2));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Energy Meter");
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        lastEnergy = energy = tag.getDouble("Energy");
        measurementPrecision = tag.getBoolean("Wh");
        lastRedstoneEnergy = tag.getInt("Redstone");
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putDouble("Energy", energy);
        tag.putBoolean("Wh", measurementPrecision);
        tag.putInt("Redstone", lastRedstoneEnergy);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EnergyMeterMenu(ModdedMenus.ENERGY_METER.get(), id, inventory, this);
    }

    public double getEnergy() {
        return energy;
    }

    public void zero() {
        energy = 0;
        notifyUpdate();
    }

    public void setPrecise(boolean value) {
        measurementPrecision = value;
        notifyUpdate();
    }
}
