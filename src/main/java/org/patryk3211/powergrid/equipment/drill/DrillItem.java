package org.patryk3211.powergrid.equipment.drill;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.item.CustomUseEffectsItem;
import net.createmod.catnip.data.TriState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.equipment.ItemBoostUtils;
import org.patryk3211.powergrid.equipment.portablebattery.BatteryUtils;

import java.util.List;

public class DrillItem extends DiggerItem implements CustomUseEffectsItem {
    public static final int MAX_SPEED_TICKS = 80;
    public static final int TICKS_PER_SPEED_LEVEL = 20;

    public DrillItem(Properties properties) {
        super(1.0f, -3.0f, Tiers.DIAMOND, BlockTags.MINEABLE_WITH_PICKAXE, properties.stacksTo(1));
    }

    public static boolean canMine(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return BatteryUtils.isBarVisible(stack, energyPerUse(), 0.3f);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return BatteryUtils.getBarWidth(stack, energyPerUse(), 0.3f);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BatteryUtils.getBarColor(stack, energyPerUse(), 0.3f);
    }

    public static int energyPerUse() {
        return ModdedConfigs.server().equipment.drillEnergyPerUse.get();
    }

    public static float baseSpeed() {
        return ModdedConfigs.server().equipment.drillMineSpeedBase.getF();
    }

    public static float bulkSpeed() {
        return ModdedConfigs.server().equipment.drillMineSpeedBulk.getF();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return canMine(state) ? baseSpeed() : 1.0F;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if(miningEntity instanceof Player player) {
            boolean boosted = ItemBoostUtils.useBoost(stack, player);
            float power = BatteryUtils.drawEnergy(player, energyPerUse() * (boosted ? 2 : 1));
            if(miningEntity instanceof PlayerDrillExtensions ext) {
                ext.powerGrid$blockDrilled(power);
            }
            if(!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F && power < 0.3f) {
                stack.hurtAndBreak(1, miningEntity, (livingEntity) -> livingEntity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
            return true;
        }
        return super.mineBlock(stack, level, state, pos, miningEntity);
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState block) {
        // Diamond level mines all
        return canMine(block);
    }

    public void leftClickEvent(Level level, BlockPos pos, ItemStack stack, Player player, boolean end) {
        if(player instanceof PlayerDrillExtensions ext) {
            ext.powerGrid$setMining(!end);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        ItemBoostUtils.addTooltip(stack, tooltipComponents);
    }

    /**
     * Copied from Create
     * @see com.simibubi.create.content.equipment.sandPaper
     */

    //Idk how much of this we need

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player.getOffhandItem().getItem() == ModdedItems.INTEGRATED_CIRCUIT.asItem()){
            if (!ItemBoostUtils.isBoosted(stack)) {
                player.startUsingItem(usedHand);
                return new InteractionResultHolder<>(InteractionResult.PASS, stack);
            }
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player))
            return;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        if (!(entityLiving instanceof Player player))
            return stack;
        var offhand = player.getOffhandItem();
        ItemBoostUtils.setBoosted(stack, true);
        offhand.hurtAndBreak(1, entityLiving, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
        return stack;
    }

    @Override
    public boolean triggerUseEffects(ItemStack stack, LivingEntity entity, int count, RandomSource random) { //prob don't need
        return false;
    }

    @Override
    public TriState shouldTriggerUseEffects(ItemStack stack, LivingEntity entity) { //prob don't need
        return CustomUseEffectsItem.super.shouldTriggerUseEffects(stack, entity);
    }

    @Override
    public SoundEvent getEatingSound() {
        return AllSoundEvents.SANDING_SHORT.getMainEvent();
    } //todo needs it own sound

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { //this only applies for primary hand, I didn't check how they do second hand
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

}
