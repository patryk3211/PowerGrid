package org.patryk3211.powergrid.equipment.drill;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.equipment.portablebattery.BatteryUtils;

public class DrillItem extends DiggerItem {
    private static final float MINE_SPEED_BASE = 6.0f;
    private static final float MINE_SPEED_BULK = 16.0f;
    public static final int MAX_SPEED_TICKS = 80;
    public static final int TICKS_PER_SPEED_LEVEL = 20;

    public static final float SPEED_BOOST = MINE_SPEED_BULK / MINE_SPEED_BASE - 1;

    public DrillItem(Properties properties) {
        super(1.0f, -3.0f, Tiers.DIAMOND, BlockTags.MINEABLE_WITH_PICKAXE, properties.stacksTo(1));
    }

    private boolean canMine(BlockState state) {
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
        return ModdedConfigs.server().electricity.drillEnergyPerUse.get();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return canMine(state) ? MINE_SPEED_BASE : 1.0F;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if(miningEntity instanceof Player player) {
            float power = BatteryUtils.drawEnergy(player, energyPerUse());
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

    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModdedTags.ingots("zinc"));
    }

    public void leftClickEvent(Level level, BlockPos pos, ItemStack stack, Player player, boolean end) {
        if(player instanceof PlayerDrillExtensions ext) {
            ext.powerGrid$setMining(!end);
        }
    }
}
