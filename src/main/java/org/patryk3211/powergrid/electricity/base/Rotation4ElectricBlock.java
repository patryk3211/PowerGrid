package org.patryk3211.powergrid.electricity.base;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.utility.ShaperUtils;

public abstract class Rotation4ElectricBlock extends DirectionalElectricBlock {
    public static final IntegerProperty ROTATION = CustomProperties.ROTATION_4;

    public Rotation4ElectricBlock(Properties settings) {
        super(settings);
    }

    public static BlockStateTerminalCollection rotation4DownTerminals(Block block, TerminalBoundingBox[] terminals, VoxelShape downShape) {
        var shapers = new VoxelShaper[] {
                VoxelShaper.forDirectional(downShape, Direction.DOWN),
                VoxelShaper.forDirectional(ShaperUtils.rotate(downShape, Direction.NORTH, Direction.EAST), Direction.DOWN),
                VoxelShaper.forDirectional(ShaperUtils.rotate(downShape, Direction.NORTH, Direction.SOUTH), Direction.DOWN),
                VoxelShaper.forDirectional(ShaperUtils.rotate(downShape, Direction.NORTH, Direction.WEST), Direction.DOWN)
        };
        return BlockStateTerminalCollection.builder(block)
                .forAllStates(state -> BlockStateTerminalCollection.each(terminals,
                        terminal -> {
                            var facing = state.getValue(FACING);
                            terminal = switch(facing) {
                                case DOWN -> terminal;
                                case UP -> terminal.rotateAroundX(180);
                                case EAST -> terminal.rotateAroundZ(-90);
                                case WEST -> terminal.rotateAroundZ(90);
                                case NORTH -> terminal.rotateAroundZ(90).rotateAroundY(90);
                                case SOUTH -> terminal.rotateAroundZ(90).rotateAroundY(-90);
                            };
                            int rotation = state.getValue(ROTATION);
                            if(facing == Direction.SOUTH) {
                                terminal = terminal.rotate(facing.getAxis(), -(90 * rotation - 90));
                            } else if(facing == Direction.EAST) {
                                terminal = terminal.rotate(facing.getAxis(), 180 - (90 * rotation - 90));
                            } else {
                                terminal = terminal.rotate(facing.getAxis(), 90 * rotation - 90);
                            }
                            return terminal;
                        })
                )
                .withShapeMapper(state -> {
                    var facing = state.getValue(FACING);
                    var rotation = state.getValue(ROTATION);
                    return shapers[rotation].get(facing);
                })
                .build();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ROTATION);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getClickedFace().getOpposite();
        int rotation = 0;
        if(facing.getAxis() == Direction.Axis.Y) {
            var player = ctx.getHorizontalDirection();
            rotation = player.get2DDataValue();
        } else {
            rotation = 1;
        }

        if(ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown())
            rotation = (rotation + 2) % 4;
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ROTATION, rotation);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        if(targetedFace.getAxis() == originalState.getValue(FACING).getAxis()) {
            return originalState.cycle(ROTATION);
        } else {
            return super.getRotatedBlockState(originalState, targetedFace);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if(state.getValue(FACING).getAxis() == Direction.Axis.Y) {
            int rotation = (state.getValue(ROTATION) + switch(rot) {
                case NONE -> 0;
                case CLOCKWISE_90 -> 1;
                case CLOCKWISE_180 -> 2;
                case COUNTERCLOCKWISE_90 -> 3;
            }) % 4;
            return state.setValue(ROTATION, rotation);
        }
        return super.rotate(state, rot);
    }
}
