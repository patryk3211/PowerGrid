package org.patryk3211.powergrid.equipment;

import com.simibubi.create.AllSoundEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class BoostingChipItem extends Item {
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
                boostChip.getOrCreateTag().put("Polishing", stack.save(new CompoundTag()));
                return new InteractionResultHolder<>(InteractionResult.PASS, boostChip);
            }
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player))
            return;
        stack.removeTagKey("Polishing");
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        if (!(entityLiving instanceof Player player))
            return stack;
        var boosted = player.getItemInHand(player.getUsedItemHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        ItemBoostUtils.setBoosted(boosted, true);
        stack.removeTagKey("Polishing");
        stack.hurtAndBreak(1, entityLiving, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
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
    public int getUseDuration(ItemStack stack) {
        return 32;
    }
}
