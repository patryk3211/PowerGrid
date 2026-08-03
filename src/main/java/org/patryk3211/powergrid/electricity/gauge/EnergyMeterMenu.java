package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class EnergyMeterMenu extends MenuBase<EnergyMeterBlockEntity> {
    public EnergyMeterMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public EnergyMeterMenu(MenuType<?> type, int id, Inventory inv, EnergyMeterBlockEntity contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    protected EnergyMeterBlockEntity createOnClient(FriendlyByteBuf extraData) {
        var world = Minecraft.getInstance().level;
        var be = world.getBlockEntity(extraData.readBlockPos());
        if(be instanceof EnergyMeterBlockEntity meter) {
            meter.readClient(extraData.readNbt());
            return meter;
        }
        return null;
    }

    @Override
    protected void initAndReadInventory(EnergyMeterBlockEntity energyMeterBlockEntity) {

    }

    @Override
    protected void addSlots() {

    }

    @Override
    protected void saveData(EnergyMeterBlockEntity energyMeterBlockEntity) {

    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
