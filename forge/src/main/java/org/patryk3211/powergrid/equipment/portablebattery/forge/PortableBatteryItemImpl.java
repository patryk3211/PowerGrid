package org.patryk3211.powergrid.equipment.portablebattery.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.equipment.portablebattery.BatteryUtils;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class PortableBatteryItemImpl {
    public static void tryTransferPower(ItemStack heldStack, ItemStack batteryStack, Player player) {
        IEnergyStorage heldEnergy = heldStack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (heldEnergy == null) return;

        int maxEnergyDrain = heldEnergy.getMaxEnergyStored() - heldEnergy.getEnergyStored();
        if (maxEnergyDrain < 1) return;


        int energyDrain = heldEnergy.receiveEnergy(maxEnergyDrain, true);
        if (energyDrain <= 0) return;

        int available = BatteryUtils.tryDrawEnergy(batteryStack, energyDrain);
        if (available <= 0) return;

        int accepted = heldEnergy.receiveEnergy(available, false);
    }
}
