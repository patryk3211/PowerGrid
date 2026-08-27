package org.patryk3211.powergrid.equipment.portablebattery.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.patryk3211.powergrid.equipment.portablebattery.BatteryUtils;

public class PortableBatteryItemImpl {
    public static void tryTransferPower(ItemStack heldStack, ItemStack batteryStack, Player player) {
        LazyOptional<IEnergyStorage> heldEnergy = heldStack.getCapability(ForgeCapabilities.ENERGY);
        heldEnergy.ifPresent(handler -> {
            int maxEnergyDrain = handler.getMaxEnergyStored() - handler.getEnergyStored();
            if (maxEnergyDrain < 1) return;

            int energyDrain = handler.receiveEnergy(maxEnergyDrain, true);
            if (energyDrain <= 0) return;

            float available = BatteryUtils.drawEnergy(player, energyDrain);
            if (available <= 0) return;

            handler.receiveEnergy((int) (available * energyDrain), false);
        });
    }
}
