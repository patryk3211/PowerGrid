package org.patryk3211.powergrid.equipment.saw;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.equipment.ItemBoostUtils;
import org.patryk3211.powergrid.equipment.portablebattery.BatteryUtils;

import java.util.List;

public class SawItem extends AxeItem {
    public SawItem(Properties properties) {
        super(Tiers.IRON, properties);
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
        return ModdedConfigs.server().equipment.sawEnergyPerUse.get();
    }

    public static float baseSpeed() {
        return ModdedConfigs.server().equipment.sawMineSpeed.getF();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return (isCorrectToolForDrops(stack, state) ? baseSpeed() : 1.0F) * (ItemBoostUtils.isBoosted(stack) ? 2 : 1);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if(miningEntity instanceof Player player) {
            boolean boosted = ItemBoostUtils.useBoost(stack, player);
            float power = BatteryUtils.drawEnergy(player, energyPerUse() * (boosted ? 2 : 1));
            if(!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F && power < 0.3f) {
                stack.hurtAndBreak(1, miningEntity, EquipmentSlot.MAINHAND);
            }
            return true;
        }
        return super.mineBlock(stack, level, state, pos, miningEntity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        ItemBoostUtils.addTooltip(stack, tooltipComponents);
    }
}
