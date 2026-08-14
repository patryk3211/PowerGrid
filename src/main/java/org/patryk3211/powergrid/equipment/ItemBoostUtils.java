package org.patryk3211.powergrid.equipment;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class ItemBoostUtils {
    public static boolean isBoosted(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getInt("Boosted") > 0;
    }

    public static void setBoosted(ItemStack stack, boolean boosted) {
        if(boosted) {
            var tag = stack.getOrCreateTag();
            tag.putInt("Boosted", (int) (stack.getMaxDamage() * 0.3f));
        } else {
            stack.removeTagKey("Boosted");
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
        if(!stack.hasTag())
            return;
        var tag = stack.getTag();
        var dmg = tag.getInt("Boosted") - 1;
        if(dmg <= 0 && tag.contains("Boosted")) {
            stack.removeTagKey("Boosted");
            if(dmg == 0)
                breakCallback.run();
            return;
        }
        tag.putInt("Boosted", dmg);
    }

    public static boolean useBoost(ItemStack stack, LivingEntity entity) {
        if(!isBoosted(stack))
            return false;
        ItemBoostUtils.damageBoost(stack, () -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @ExpectPlatform
    public static Recipe<?> findRecipe(Level level, ItemStack chip, ItemStack toBoost) {
        throw new AssertionError();
    }
}
