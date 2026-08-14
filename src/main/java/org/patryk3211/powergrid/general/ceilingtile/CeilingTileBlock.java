package org.patryk3211.powergrid.general.ceilingtile;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlocks;

import java.util.List;
import java.util.function.Predicate;

public class CeilingTileBlock extends Block implements CeilingBlock {
    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 2, 16);

    public CeilingTileBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(placementHelper(state, level, pos, player, hand, hit) == InteractionResult.SUCCESS)
            return InteractionResult.SUCCESS;
        var stack = player.getMainHandItem();
        if (ModdedBlocks.FACTORY_LIGHT.is(stack.getItem())) {
            if(!player.isCreative())
                stack.shrink(1);
            if(!level.isClientSide) {
                level.setBlockAndUpdate(pos, ModdedBlocks.CEILING_TILE_LAMP.getDefaultState());
                level.playSound(null, pos,
                        ModdedBlocks.FACTORY_LIGHT.getDefaultState().getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.25f + 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        if (ModdedBlocks.WIRE_CONNECTOR.is(stack.getItem())) {
            if(!player.isCreative())
                stack.shrink(1);
            if(!level.isClientSide) {
                level.setBlockAndUpdate(pos, ModdedBlocks.CEILING_TILE_CONNECTOR.getDefaultState());
                level.playSound(null, pos,
                        ModdedBlocks.WIRE_CONNECTOR.getDefaultState().getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.25f + 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        if (ModdedBlocks.CORD_JUNCTION.is(stack.getItem())) {
            if(!player.isCreative())
                stack.shrink(1);
            if(!level.isClientSide) {
                level.setBlockAndUpdate(pos, ModdedBlocks.CEILING_TILE_JUNCTION.getDefaultState());
                level.playSound(null, pos,
                        ModdedBlocks.CORD_JUNCTION.getDefaultState().getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.25f + 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        if (ModdedBlocks.SOLAR_PANEL.is(stack.getItem())) {
            if(!player.isCreative())
                stack.shrink(1);
            if(!level.isClientSide) {
                level.setBlockAndUpdate(pos, ModdedBlocks.CEILING_TILE_SOLAR.getDefaultState());
                level.playSound(null, pos,
                        ModdedBlocks.SOLAR_PANEL.getDefaultState().getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.25f + 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper implements IPlacementHelper {
        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return ModdedBlocks.CEILING_TILE::isIn;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return s -> s.getBlock() instanceof CeilingBlock;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
                                         BlockHitResult ray) {
            List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(),
                    ray.getDirection().getAxis(), dir -> dir.getAxis() != Direction.Axis.Y && world.getBlockState(pos.relative(dir)).canBeReplaced());

            if (directions.isEmpty())
                return PlacementOffset.fail();
            else {
                return PlacementOffset.success(pos.relative(directions.get(0)), s -> s.getBlock().defaultBlockState());
            }
        }
    }
}
