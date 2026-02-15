/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.kinetics.generator.inductionrotor;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.*;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.kinetics.generator.rotor.AbstractRotorBlock;
import org.patryk3211.powergrid.utility.Directions;

@MethodsReturnNonnullByDefault
public class CommutatorBlock extends AbstractRotorBlock implements IBE<CommutatorBlockEntity>, IElectric, IBrushPlacement {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final BlockStateTerminalCollection terminals;
    private final ImmutableMap<BlockState, VoxelShape> outlines;

    private static final TerminalBoundingBox[] TERMINALS_HORIZONTAL = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 0, 14, 6, 3, 16, 9)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 13, 14, 7, 16, 16, 10)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    public CommutatorBlock(Properties properties) {
        super(properties);
        var baseShaper = VoxelShaper.forHorizontalAxis(Shapes.or(
                box(0, 0, 3, 16, 12, 13),
                box(0, 12, 6, 3, 16, 9),
                box(13, 12, 7, 16, 16, 10)
        ), Direction.Axis.Z);
        terminals = BlockStateTerminalCollection.builder(this)
                .forAllStatesExcept(state -> {
                    var facing = state.getValue(HORIZONTAL_FACING);
                    return BlockStateTerminalCollection.each(TERMINALS_HORIZONTAL, terminal -> terminal
                            .rotateAroundY((int) facing.toYRot() - 180));
                })
                .withShapeMapper(state -> {
                    var axis = state.getValue(HORIZONTAL_FACING).getAxis();
                    return baseShaper.get(axis);
                })
                .build();
        outlines = getShapeForEachState(terminals.shapeMapper());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return outlines.get(state);
    }

    @Override
    public Class<CommutatorBlockEntity> getBlockEntityClass() {
        return CommutatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CommutatorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.GENERATOR_COMMUTATOR.get();
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return terminals.get(state, index);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var result = super.onWrenched(state, context);
        if(result == InteractionResult.SUCCESS && !context.getLevel().isClientSide)
            ElectricBlock.refreshConnectionEntities(context.getLevel(), context.getClickedPos());
        return result;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Direction.Axis preferredAxis = null;
        for(Direction.Axis axis : Directions.HORIZONTAL_AXIS) {
            if(hasPositive(world, pos, axis) ||
                    hasNegative(world, pos, axis)) {
                if(preferredAxis != null) {
                    preferredAxis = null;
                    break;
                }
                preferredAxis = axis;
            }
        }

        Direction facing = null;
        if(preferredAxis != null) {
            for (var dir : context.getNearestLookingDirections()) {
                if(dir.getAxis() == preferredAxis) {
                    facing = dir;
                    break;
                }
            }
        }
        if(facing == null) {
            facing = context.getHorizontalDirection();
        }

        return defaultBlockState()
                .setValue(HORIZONTAL_FACING, facing);
    }

    @Override
    public boolean canConnect(BlockState state, Direction dir) {
        return state.getValue(HORIZONTAL_FACING).getAxis() == dir.getAxis();
    }

    @Override
    public Direction.@NotNull Axis getAssemblyRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Vec3 brushOffset(BlockState state) {
        return switch (state.getValue(HORIZONTAL_FACING).getAxis()) {
            case Z -> new Vec3(3.5 / 16f, 0, 2 / 16f);
            case X -> new Vec3(-2 / 16f, 0, 3.5 / 16);
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public Vec3 sparkVelocity(BlockState state, float angularVelocity) {
        return switch(state.getValue(HORIZONTAL_FACING).getAxis()) {
            case Z -> new Vec3(0, 1 / 4f + Math.abs(angularVelocity) / 100f, 0);
            case X -> new Vec3(0, -1 / 4f - Math.abs(angularVelocity) / 100f, 0);
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(HORIZONTAL_FACING, rot.rotate(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(HORIZONTAL_FACING)));
    }
}
