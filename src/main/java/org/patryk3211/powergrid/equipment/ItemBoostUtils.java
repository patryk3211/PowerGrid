package org.patryk3211.powergrid.equipment;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.collections.ModdedDataComponents;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class ItemBoostUtils {
    public static boolean isBoosted(ItemStack stack) {
        var data = stack.get(ModdedDataComponents.BOOST.get());
        return data != null && data.durability() > 0;
    }

    public static void setBoosted(ItemStack stack, boolean boosted) {
        if(boosted) {
            stack.set(ModdedDataComponents.BOOST.get(), BoostData.of((int) (stack.getMaxDamage() * 0.3f)));
        } else {
            stack.remove(ModdedDataComponents.BOOST.get());
        }
    }

    public static void addTooltip(ItemStack stack, List<Component> tooltip) {
        if(isBoosted(stack)) {
            Lang.translate("tooltip.boosted")
                    .style(ChatFormatting.BLUE).style(ChatFormatting.ITALIC)
                    .addTo(tooltip);
        }
    }

    public static void damageBoost(ItemStack stack, Runnable breakCallback) {
        var data = stack.get(ModdedDataComponents.BOOST.get());
        if(data == null)
            return;
        var dmg = data.durability() - 1;
        if(dmg <= 0) {
            stack.remove(ModdedDataComponents.BOOST.get());
            if(dmg == 0)
                breakCallback.run();
            return;
        }
        stack.set(ModdedDataComponents.BOOST.get(), BoostData.of(dmg));
    }

    public static boolean useBoost(ItemStack stack, LivingEntity entity) {
        if(!isBoosted(stack))
            return false;
        ItemBoostUtils.damageBoost(stack, () -> entity.onEquippedItemBroken(stack.getItem(), EquipmentSlot.MAINHAND));
        return true;
    }

    @ExpectPlatform
    public static Recipe<?> findRecipe(Level level, ItemStack chip, ItemStack toBoost) {
        throw new AssertionError();
    }
}
