package org.patryk3211.powergrid.equipment;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.item.CustomUseEffectsItem;
import net.createmod.catnip.data.TriState;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.collections.ModdedAdvancements;

public class BoostingChipItem extends Item implements CustomUseEffectsItem {
    public BoostingChipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack boostChip = player.getItemInHand(usedHand);
        ItemStack stack = player.getItemInHand(usedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        var recipe = ItemBoostUtils.findRecipe(level, boostChip, stack);
        if (recipe != null) {
            if (!ItemBoostUtils.isBoosted(stack)) {
                player.startUsingItem(usedHand);
                return new InteractionResultHolder<>(InteractionResult.PASS, boostChip);
            }
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player))
            return stack;
        var boosted = player.getItemInHand(player.getUsedItemHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        ItemBoostUtils.setBoosted(boosted, true);
        if(!ModdedAdvancements.BOOSTING_CHIP.isAlreadyAwardedTo(player)) {
            ModdedAdvancements.BOOSTING_CHIP.awardTo(player);
        }
        entity.onEquippedItemBroken(stack.getItem(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        stack.shrink(1);
        return stack;
    }

    @Override
    public SoundEvent getEatingSound() {
        return AllSoundEvents.SANDING_SHORT.getMainEvent();
    } //todo needs it own sound

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public TriState shouldTriggerUseEffects(ItemStack stack, LivingEntity entity) {
        return TriState.TRUE;
    }

    @Override
    public boolean triggerUseEffects(ItemStack stack, LivingEntity entity, int i, RandomSource random) {
        if((entity.getTicksUsingItem() - 6) % 7 == 0) {
            entity.playSound(entity.getEatingSound(stack), 0.9F + 0.2F * random.nextFloat(), random.nextFloat() * 0.2F + 0.9F);
        }
        return true;
    }
}
