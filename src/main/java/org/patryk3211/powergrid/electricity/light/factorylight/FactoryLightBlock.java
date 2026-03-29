package org.patryk3211.powergrid.electricity.light.factorylight;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalAxisElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.wire.powercord.IAcceptCord;

public class FactoryLightBlock extends HorizontalAxisElectricBlock implements IAcceptCord, IAcceptConnector, IBE<FactoryLightBlockEntity> {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 3);

    public FactoryLightBlock(Properties settings) {
        super(settings.lightLevel(state -> switch(state.getValue(POWER)) {
            case 2 -> 10;
            case 3 -> 15;
            default -> 0;
        }));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART, POWER);
    }

    private static boolean canConnect(Direction dir, BlockState neighbour) {
        int part = neighbour.getValue(PART);
        if(part == 0) {
            // Singular piece can always be extended
            return true;
        }
        if(part == 2) {
            // Cannot connect third lamp into a central piece
            return false;
        }
        if(neighbour.getValue(HORIZONTAL_AXIS) != dir.getAxis()) {
            // Wrong axis, cannot extend
            return false;
        }
        if(part == 1 && dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            // Broken state?
            return false;
        }
        if(part == 3 && dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            // Broken state?
            return false;
        }
        // Remaining states should be fine to extend.
        return true;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var dir = ctx.getClickedFace();
        var clickedState = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(dir, -1));
        if(clickedState.is(this) && dir.getAxis() != Direction.Axis.Y) {
            if(canConnect(dir, clickedState)) {
                return defaultBlockState()
                        .setValue(HORIZONTAL_AXIS, dir.getAxis())
                        .setValue(PART, dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 3 : 1);
            }
        }
        return super.getStateForPlacement(ctx);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        int part = state.getValue(PART);
        if(neighborState.is(this)) {
            if(neighborState.getValue(HORIZONTAL_AXIS) == direction.getAxis()) {
                int nPart = neighborState.getValue(PART);
                var axisDir = direction.getAxisDirection();
                if(part == 0 && axisDir == Direction.AxisDirection.POSITIVE && (nPart == 3 || nPart == 2)) {
                    return state
                            .setValue(HORIZONTAL_AXIS, direction.getAxis())
                            .setValue(PART, 1);
                } else if(part == 0 && axisDir == Direction.AxisDirection.NEGATIVE && (nPart == 1 || nPart == 2)) {
                    return state
                            .setValue(HORIZONTAL_AXIS, direction.getAxis())
                            .setValue(PART, 3);
                } else if(part == 1 && axisDir == Direction.AxisDirection.NEGATIVE && (nPart == 1 || nPart == 2)) {
                    return state.setValue(PART, 2);
                } else if(part == 3 && axisDir == Direction.AxisDirection.POSITIVE && (nPart == 3 || nPart == 2)) {
                    return state.setValue(PART, 2);
                }
            }
        } else if(neighborState.is(Blocks.AIR)) {
            var axis = state.getValue(HORIZONTAL_AXIS);
            if(axis == direction.getAxis()) {
                if(part == 1 && direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    return state.setValue(PART, 0);
                } else if(part == 3 && direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                    return state.setValue(PART, 0);
                } else if(part == 2) {
                    if(direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                        return state.setValue(PART, 3);
                    } else {
                        return state.setValue(PART, 1);
                    }
                }
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
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
