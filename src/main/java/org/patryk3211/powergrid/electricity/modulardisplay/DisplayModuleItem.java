package org.patryk3211.powergrid.electricity.modulardisplay;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.electricity.info.Current;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;

public class DisplayModuleItem extends Item implements IHaveElectricProperties {
    public DisplayModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.coil(25f, player, tooltip);
        Current.min(0.5f, player, tooltip);
        Power.max(25f, player, tooltip);
    }
}
