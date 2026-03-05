package org.patryk3211.powergrid.equipment.drill;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedTags;

public class DrillItem extends DiggerItem {
    private static final float MINE_SPEED_BASE = 4.0f;
    private static final float MINE_SPEED_POWERED = 8.0f;
    private static final float MINE_SPEED_BULK = 12.0f;
    public static final int MAX_SPEED_TICKS = 60;

    public static final float SPEED_BOOST = MINE_SPEED_BULK / MINE_SPEED_POWERED - 1;

    public DrillItem(Properties properties) {
        super(1.0f, -3.0f, Tiers.DIAMOND, BlockTags.MINEABLE_WITH_PICKAXE, properties.stacksTo(1));
    }

    private boolean canMine(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // TODO: Check if powered by battery
        return canMine(state) ? MINE_SPEED_BASE : 1.0F;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if(miningEntity instanceof PlayerDrillExtensions ext) {
            ext.powerGrid$blockDrilled();
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
