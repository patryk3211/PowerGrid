package org.patryk3211.powergrid.general.ceilingtile;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.collections.ModdedBlocks;

public interface CeilingBlock extends IWrenchable {
    default InteractionResult removeCeilingAttachment(UseOnContext context, ItemStack returnedAttachment) {
        var player = context.getPlayer();
        if(player != null && !player.isCreative())
            player.addItem(returnedAttachment);

        var level = context.getLevel();
        if(!level.isClientSide) {
            level.setBlockAndUpdate(context.getClickedPos(), ModdedBlocks.CEILING_TILE.getDefaultState());
            IWrenchable.playRemoveSound(level, context.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }

    default InteractionResult placementHelper(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        var stack = player.getMainHandItem();
        IPlacementHelper placementHelper = PlacementHelpers.get(CeilingTileBlock.placementHelperId);
        if(!player.isShiftKeyDown() && player.mayBuild()) {
            if(placementHelper.matchesItem(stack)) {
                placementHelper.getOffset(player, level, state, pos, hit)
                        .placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hit);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
