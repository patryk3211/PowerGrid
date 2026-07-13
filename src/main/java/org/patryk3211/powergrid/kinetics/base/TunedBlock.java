package org.patryk3211.powergrid.kinetics.base;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class TunedBlock extends ElectricKineticBlock {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty BASE = BooleanProperty.create("base");

    public TunedBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BASE, true));
    }

    public static BlockStateTerminalCollection tunedNorthTerminals(Block block, TerminalBoundingBox[] terminals, VoxelShape northShapeBased, VoxelShape northShapeBaseless) {
        var shaperBased = VoxelShaper.forHorizontal(northShapeBased, Direction.NORTH);
        var shaperBaseless = VoxelShaper.forHorizontal(northShapeBaseless, Direction.NORTH);
        return BlockStateTerminalCollection.builder(block)
                .forAllStates(state -> BlockStateTerminalCollection.each(terminals, terminal -> switch(state.getValue(HORIZONTAL_FACING)) {
                    case NORTH -> terminal;
                    case SOUTH -> terminal.rotateAroundY(180);
                    case EAST -> terminal.rotateAroundY(90);
                    case WEST -> terminal.rotateAroundY(-90);
                    default -> throw new IllegalStateException();
                }))
                .withShapeMapper(state -> state.getValue(BASE)
                        ? shaperBased.get(state.getValue(HORIZONTAL_FACING))
                        : shaperBaseless.get(state.getValue(HORIZONTAL_FACING)))
                .build();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING, BASE);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        if(!state.getValue(BASE) && face == Direction.DOWN)
            return true;
        return face == Direction.UP;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = this.defaultBlockState()
                .setValue(HORIZONTAL_FACING, context.getHorizontalDirection()
                        .getOpposite());
        var belowPos = context.getClickedPos().below();
        var level = context.getLevel();
        var below = level.getBlockState(belowPos);
        if(below.getBlock() instanceof KineticBlock kinetic && kinetic.hasShaftTowards(level, belowPos, below, Direction.UP)) {
            state = state.setValue(BASE, false);
        }
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(HORIZONTAL_FACING, rot.rotate(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        if(targetedFace == Direction.DOWN) {
            return originalState.cycle(BASE);
        }
        return super.getRotatedBlockState(originalState, targetedFace);
    }
}
