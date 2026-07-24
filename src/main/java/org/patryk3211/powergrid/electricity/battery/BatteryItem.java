package org.patryk3211.powergrid.electricity.battery;

import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;

public class BatteryItem extends BlockItem {
    public BatteryItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        InteractionResult initialResult = super.place(ctx);
        if (!initialResult.consumesAction())
            return initialResult;
        tryMultiPlace(ctx);
        return initialResult;
    }

    private void tryMultiPlace(BlockPlaceContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null)
            return;
        if (player.isShiftKeyDown())
            return;
        Direction face = ctx.getClickedFace();
        ItemStack stack = ctx.getItemInHand();
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockPos placedOnPos = pos.relative(face.getOpposite());
        BlockState placedOnState = world.getBlockState(placedOnPos);
        BlockEntity be = world.getBlockEntity(placedOnPos);
        if (be instanceof MultiBlockBatteryEntity batteryBe) {
            if (!BatteryBlock.isBattery(placedOnState))
                return;
            if (SymmetryWandItem.presentInHotbar(player))
                return;
            MultiBlockBatteryEntity batteryAt = CustomConnectivityHandler.partAt(ModdedBlockEntities.MULTIBLOCK_BATTERY.get(),
                    ((MultiBlockBatteryEntity) be).getSpec(), world, placedOnPos);
            if (batteryAt == null)
                return;
            MultiBlockBatteryEntity controllerBE = batteryAt.getControllerBE();
            if (controllerBE == null)
                return;

            int width = controllerBE.width;
            if (width == 1)
                return;

            int batteriesToPlace = 0;
            BlockPos startPos = face == Direction.DOWN ? controllerBE.getBlockPos()
                    .below()
                    : controllerBE.getBlockPos()
                    .above(controllerBE.height);

            if (startPos.getY() != pos.getY())
                return;

            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                    BlockState blockState = world.getBlockState(offsetPos);
                    if (BatteryBlock.isBattery(blockState))
                        continue;
                    if (!blockState.canBeReplaced())
                        return;
                    batteriesToPlace++;
                }
            }

            if (!player.isCreative() && stack.getCount() < batteriesToPlace)
                return;

            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                    BlockState blockState = world.getBlockState(offsetPos);
                    if (BatteryBlock.isBattery(blockState))
                        continue;
                    BlockPlaceContext context = BlockPlaceContext.at(ctx, offsetPos, face);
                    super.place(context);
                }
            }
        }
    }
}
