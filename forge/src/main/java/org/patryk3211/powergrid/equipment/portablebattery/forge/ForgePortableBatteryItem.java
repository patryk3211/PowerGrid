package org.patryk3211.powergrid.equipment.portablebattery.forge;

import com.simibubi.create.AllEnchantments;
import com.simibubi.create.content.equipment.armor.BacktankItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.patryk3211.powergrid.equipment.portablebattery.PortableBatteryItem;

import java.util.function.Supplier;

public class ForgePortableBatteryItem extends PortableBatteryItem {
    public ForgePortableBatteryItem(Holder<ArmorMaterial> material, Properties settings, ResourceLocation textureLoc, Supplier<BacktankItem.BacktankBlockItem> placeable) {
        super(material, settings, textureLoc, placeable);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(AllEnchantments.CAPACITY);
    }
}
