package org.patryk3211.powergrid.general.ceilingtile;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
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
}
