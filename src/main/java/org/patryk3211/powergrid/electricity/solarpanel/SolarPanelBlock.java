package org.patryk3211.powergrid.electricity.solarpanel;

import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;

import java.util.List;
import java.util.function.Predicate;

public class SolarPanelBlock extends DirectionalElectricBlock implements IBE<SolarPanelBlockEntity>,IHaveElectricProperties, IAcceptConnector {
    private static final VoxelShape SHAPE = box(0, 6, 0, 16, 10, 16);

    private static final int placementHelperId = PlacementHelpers.register(new SolarPanelBlock.PlacementHelper());

    public SolarPanelBlock(Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        var shaper = VoxelShaper.forDirectional(SHAPE, Direction.UP);
        return shaper.get(state.getValue(SolarPanelBlock.FACING));
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        //todo once diode is implemented add Voc, Isc, Imp?, Vmp?, Pmax?
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack heldItem = player.getItemInHand(hand);
        IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
        if (!player.isShiftKeyDown() && player.mayBuild()) {
            if (placementHelper.matchesItem(heldItem)) {
                var offset = placementHelper.getOffset(player, level, state, pos, hit);
                offset.placeInWorld(level, (BlockItem) heldItem.getItem(), player, hand, hit);
                if(level.getBlockEntity(pos) instanceof SolarPanelBlockEntity be && be.canAccept()) {
                    var newBE = level.getBlockEntity(offset.getBlockPos());
                    if(newBE instanceof SolarPanelBlockEntity panel) {
                        be.getController()
                                .ifPresent(controller -> controller.connect(panel));
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public Class<SolarPanelBlockEntity> getBlockEntityClass() {
        return SolarPanelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SolarPanelBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.SOLAR_PANEL.get();
    }

    @Override
    public boolean isPolarized() {
        return true;
    }

    public static Direction getInteractionDirection(Direction facing, UseOnContext context) {
        var pos = context.getClickedPos();
        var clickLoc = context.getClickLocation();
        var relativeLoc = clickLoc.subtract(Vec3.atCenterOf(pos));
        Direction maxDir = null;
        double maxMag = 2.0 / 16;
        for(var dir : Direction.values()) {
            if(dir.getAxis() == facing.getAxis())
                continue;
            double mag = relativeLoc.get(dir.getAxis());
            if(dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                if(mag < 0)
                    mag = 0;
            } else {
                if(mag > 0)
                    mag = 0;
                mag = -mag;
            }
            if(mag > maxMag) {
                maxMag = mag;
                maxDir = dir;
            }
        }
        return maxDir;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var facing = state.getValue(FACING);
        if(context.getClickedFace().getAxis() != facing.getAxis())
            return super.onWrenched(state, context);
        var pos = context.getClickedPos();
        Direction maxDir = getInteractionDirection(facing, context);
        if(maxDir == null)
            return super.onWrenched(state, context);
        // Split or merge panels.
        var level = context.getLevel();
        if(!(level.getBlockEntity(pos) instanceof SolarPanelBlockEntity thisSolarBE))
            return InteractionResult.FAIL;
        if(!(level.getBlockEntity(pos.relative(maxDir)) instanceof SolarPanelBlockEntity neighborBE))
            return InteractionResult.FAIL;
        if(SolarPanelBlockEntity.areConnected(thisSolarBE, neighborBE)) {
            var controller = thisSolarBE.getController();
            if(controller.isEmpty())
                return InteractionResult.FAIL;
            SolarPanelBlockEntity.splitMultiblock(controller.get(), pos.get(maxDir.getAxis()), maxDir);
            if(level.isClientSide)
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        } else {
            var controller1 = thisSolarBE.getController();
            var controller2 = neighborBE.getController();
            if(controller1.isEmpty() || controller2.isEmpty())
                return InteractionResult.FAIL;
            SolarPanelBlockEntity.mergeMultiblock(controller1.get(), controller2.get());
            if(level.isClientSide)
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper implements IPlacementHelper {
        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return i -> ModdedBlocks.SOLAR_PANEL.isIn(i);
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
