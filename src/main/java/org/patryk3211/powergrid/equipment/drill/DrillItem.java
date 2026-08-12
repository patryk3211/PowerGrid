package org.patryk3211.powergrid.equipment.drill;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.equipment.ItemBoostUtils;
import org.patryk3211.powergrid.equipment.PGToolMaterials;
import org.patryk3211.powergrid.equipment.portablebattery.BatteryUtils;

import java.util.List;

public class DrillItem extends DiggerItem {
    public static final int MAX_SPEED_TICKS = 80;
    public static final int TICKS_PER_SPEED_LEVEL = 20;

    public DrillItem(Properties properties) {
        super(PGToolMaterials.ZINC_DRILL, BlockTags.MINEABLE_WITH_PICKAXE, properties.stacksTo(1));
    }

    public static boolean canMine(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return BatteryUtils.isBarVisible(stack, energyPerUse());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return BatteryUtils.getBarWidth(stack, energyPerUse());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BatteryUtils.getBarColor(stack, energyPerUse());
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
            if(!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F && power == 0) {
                Tool tool = stack.get(DataComponents.TOOL);
                if(tool != null)
                    stack.hurtAndBreak(tool.damagePerBlock(), miningEntity, EquipmentSlot.MAINHAND);
            }
            return true;
        }
        return super.mineBlock(stack, level, state, pos, miningEntity);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        // Diamond level mines all
        return canMine(state);
    }

    public void leftClickEvent(Level level, BlockPos pos, ItemStack stack, Player player, boolean end) {
        if(player instanceof PlayerDrillExtensions ext) {
            ext.powerGrid$setMining(!end);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        ItemBoostUtils.addTooltip(stack, tooltipComponents);
    }
}
