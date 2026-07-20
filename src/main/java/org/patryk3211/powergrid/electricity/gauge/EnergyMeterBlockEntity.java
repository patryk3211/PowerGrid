package org.patryk3211.powergrid.electricity.gauge;

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
import org.patryk3211.powergrid.collections.ModdedMenus;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

public class EnergyMeterBlockEntity extends ElectricBlockEntity implements MenuProvider {
    private ElectricWire series;
    private ElectricWire shunt;

    double lastEnergy;
    double energy;

    public EnergyMeterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
        setUnsaved();
    }

    @Override
    public void tick() {
        super.tick();
        lastEnergy = energy;
        energy += series.current() * shunt.potentialDifference() * 0.05 / 3_600_000;
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
        energy = tag.getDouble("Energy");
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putDouble("Energy", energy);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EnergyMeterMenu(ModdedMenus.ENERGY_METER.get(), id, inventory, this);
    }
}
