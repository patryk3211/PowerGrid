package org.patryk3211.powergrid.electricity.solarpanel;

import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;

import java.util.List;
import java.util.function.Predicate;

public class SolarPanelBlock extends Rotation4ElectricBlock implements IBE<MultiBlockSolarPanelEntity>,IHaveElectricProperties, IAcceptConnector {

    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 6, 0, 16, 10, 16)
    );

    private static final int placementHelperId = PlacementHelpers.register(new SolarPanelBlock.PlacementHelper());

    private static final TerminalBoundingBox[] NORTH_TERMINALS = new TerminalBoundingBox[] {
    };

    public SolarPanelBlock(Properties settings) {
        super(settings);
        setTerminalCollection(rotation4DownTerminals(this, NORTH_TERMINALS, SHAPE));
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {

    }

    //todo fix failed implementation of IMultiBlockSolarPanel
//    @Override
//    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
//        if(oldState.getBlock() == state.getBlock())
//            return;
//        if(moved)
//            return;
//        withBlockEntityDo(world, pos, MultiBlockSolarPanelEntity::queueConnectivityUpdate);
//    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack heldItem = player.getItemInHand(hand);
        IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
        if (!player.isShiftKeyDown() && player.mayBuild()) {
            if (placementHelper.matchesItem(heldItem)) {
                placementHelper.getOffset(player, level, state, pos, hit)
                        .placeInWorld(level, (BlockItem) heldItem.getItem(), player, hand, hit);
                return InteractionResult.SUCCESS;
            }
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public Class<MultiBlockSolarPanelEntity> getBlockEntityClass() {
        return MultiBlockSolarPanelEntity.class;
    }

    @Override
    public BlockEntityType<? extends MultiBlockSolarPanelEntity> getBlockEntityType() {
        return ModdedBlockEntities.SOLAR_PANEL.get();
    }

    @Override
    public boolean isPolarized() {
        return true;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var be = context.getLevel().getBlockEntity(context.getClickedPos());
        if (be instanceof SolarPanelBlockEntity blockEntity) {
            blockEntity.getPlacedBlockRotation();
        }
        return super.onWrenched(state, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper implements IPlacementHelper {
        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return i -> ModdedBlocks.SOLAR_PANEL.isIn(i) || ModdedBlocks.SOLAR_PANEL.isIn(i);
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return s -> s.getBlock() instanceof SolarPanelBlock;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
                                         BlockHitResult ray) {
            List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(),
                    state.getValue(SolarPanelBlock.FACING)
                            .getAxis(),
                    dir -> world.getBlockState(pos.relative(dir))
                            .canBeReplaced());

            if (directions.isEmpty())
                return PlacementOffset.fail();
            else {
                return PlacementOffset.success(pos.relative(directions.get(0)),
                        s -> s.setValue(FACING, state.getValue(FACING)));
            }
        }
    }

}
