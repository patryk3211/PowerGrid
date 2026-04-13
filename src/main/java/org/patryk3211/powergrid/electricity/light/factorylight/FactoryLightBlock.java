package org.patryk3211.powergrid.electricity.light.factorylight;

import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalAxisElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;
import org.patryk3211.powergrid.electricity.wire.powercord.IAcceptCord;

public class FactoryLightBlock extends HorizontalAxisElectricBlock implements IAcceptCord, IAcceptConnector, IBE<FactoryLightBlockEntity> {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 6);
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 3);

    public static final VoxelShape SHAPE_SINGLE = box(2, 8, 2, 14, 16, 14);
    public static final VoxelShaper SHAPER_ENDS = VoxelShaper.forHorizontal(box(2, 8, 2, 14, 16, 16), Direction.NORTH);
    public static final VoxelShaper SHAPER_MIDDLE = VoxelShaper.forHorizontalAxis(box(2, 8, 0, 14, 16, 16), Direction.Axis.Z);

    public FactoryLightBlock(Properties settings) {
        super(settings.noOcclusion().lightLevel(state -> switch(state.getValue(POWER)) {
            case 2 -> 10;
            case 3 -> 15;
            default -> 0;
        }));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch(state.getValue(PART)) {
            case 0 -> SHAPE_SINGLE;
            case 1 -> SHAPER_ENDS.get(Direction.NORTH);
            case 2 -> SHAPER_MIDDLE.get(Direction.Axis.Z);
            case 3 -> SHAPER_ENDS.get(Direction.SOUTH);
            case 4 -> SHAPER_ENDS.get(Direction.WEST);
            case 5 -> SHAPER_MIDDLE.get(Direction.Axis.X);
            case 6 -> SHAPER_ENDS.get(Direction.EAST);
            default -> throw new IllegalStateException();
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART, POWER);
    }

    private static boolean canConnect(Direction dir, BlockState neighbour) {
        int part = neighbour.getValue(PART);
        var axis = neighbour.getValue(HORIZONTAL_AXIS);

        if (part == 0) {
            return true;
        }

        // Determine valid range based on axis
        boolean isZ = axis == Direction.Axis.Z;
        boolean isX = axis == Direction.Axis.X;

        if (isZ && (part < 1 || part > 3)) return false;
        if (isX && (part < 4 || part > 6)) return false;

        // Center pieces cannot be extended
        if ((isZ && part == 2) || (isX && part == 5)) {
            return false;
        }

        if (axis != dir.getAxis()) {
            return false;
        }

        // Edge blocking logic
        if ((part == 1 || part == 4) && dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            return false;
        }

        if ((part == 3 || part == 6) && dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            return false;
        }

        return true;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var dir = ctx.getClickedFace();
        var clickedState = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(dir, -1));

        if (clickedState.is(this) && dir.getAxis() != Direction.Axis.Y) {
            if (canConnect(dir, clickedState)) {

                int part;
                if (dir.getAxis() == Direction.Axis.Z) {
                    part = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 3 : 1;
                } else {
                    part = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 6 : 4;
                }

                return defaultBlockState()
                        .setValue(HORIZONTAL_AXIS, dir.getAxis())
                        .setValue(PART, part);
            }
        }

        return super.getStateForPlacement(ctx);
    }
    public static boolean isCenter(int part) {
        return part == 2 || part == 5;
    }

    public static boolean isNegativeEdge(int part) {
        return part == 1 || part == 4;
    }

    public static boolean isPositiveEdge(int part) {
        return part == 3 || part == 6;
    }

    private static int getCenter(Direction.Axis axis) {
        return axis == Direction.Axis.Z ? 2 : 5;
    }

    private static int getNegativeEdge(Direction.Axis axis) {
        return axis == Direction.Axis.Z ? 1 : 4;
    }

    private static int getPositiveEdge(Direction.Axis axis) {
        return axis == Direction.Axis.Z ? 3 : 6;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        int part = state.getValue(PART);

        if (neighborState.is(this)) {
            if (neighborState.getValue(HORIZONTAL_AXIS) == direction.getAxis()) {

                int nPart = neighborState.getValue(PART);
                var axis = direction.getAxis();
                var axisDir = direction.getAxisDirection();

                if (part == 0 && axisDir == Direction.AxisDirection.POSITIVE &&
                        (isPositiveEdge(nPart) || isCenter(nPart))) {

                    return state.setValue(HORIZONTAL_AXIS, axis)
                            .setValue(PART, getNegativeEdge(axis));

                } else if (part == 0 && axisDir == Direction.AxisDirection.NEGATIVE &&
                        (isNegativeEdge(nPart) || isCenter(nPart))) {

                    return state.setValue(HORIZONTAL_AXIS, axis)
                            .setValue(PART, getPositiveEdge(axis));

                } else if (isNegativeEdge(part) && axisDir == Direction.AxisDirection.NEGATIVE &&
                        (isNegativeEdge(nPart) || isCenter(nPart))) {

                    return state.setValue(PART, getCenter(axis));

                } else if (isPositiveEdge(part) && axisDir == Direction.AxisDirection.POSITIVE &&
                        (isPositiveEdge(nPart) || isCenter(nPart))) {

                    return state.setValue(PART, getCenter(axis));
                }
            }

        } else if (neighborState.is(Blocks.AIR)) {

            var axis = state.getValue(HORIZONTAL_AXIS);

            if (axis == direction.getAxis()) {

                if (isNegativeEdge(part) && direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    return state.setValue(PART, 0);

                } else if (isPositiveEdge(part) && direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                    return state.setValue(PART, 0);

                } else if (isCenter(part)) {
                    if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                        return state.setValue(PART, getPositiveEdge(axis));
                    } else {
                        return state.setValue(PART, getNegativeEdge(axis));
                    }
                }
            }
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        var stack = player.getItemInHand(hand);
        if(stack.isEmpty() || stack.getItem() instanceof ILightBulb) {
            return onBlockEntityUse(world, pos, be ->
                    be.replaceBulb(player, hand, stack)
                            ? InteractionResult.SUCCESS
                            : InteractionResult.FAIL);
        } else {
            // Holding something else.
            return InteractionResult.PASS;
        }
    }

    @Override
    public boolean renderPlug() {
        return true;
    }

    @Override
    public Class<FactoryLightBlockEntity> getBlockEntityClass() {
        return FactoryLightBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FactoryLightBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.FACTORY_LIGHT.get();
    }
}
