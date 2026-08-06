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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.kinetics.generator.rotor.AbstractRotorBlock;

public class VerticalCommutatorBlock extends AbstractRotorBlock implements IBE<CommutatorBlockEntity>, ICommutator {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty UP = BlockStateProperties.UP;

    private final BlockStateTerminalCollection terminals;
    private final BlockStateTerminalCollection terminalsFlipped;
    private final ImmutableMap<BlockState, VoxelShape> outlines;

    private static final TerminalBoundingBox[] TERMINALS_HORIZONTAL = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 0, 14, 6, 3, 16, 9)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 13, 14, 7, 16, 16, 10)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private static final TerminalBoundingBox[] TERMINALS_HORIZONTAL_FLIPPED = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 0, 14, 6, 3, 16, 9)
                    .withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 13, 14, 7, 16, 16, 10)
                    .withColor(IDecoratedTerminal.RED)
    };

    public VerticalCommutatorBlock(Properties properties) {
        super(properties);
        var shaperUp = VoxelShaper.forHorizontalAxis(Shapes.or(
                box(0, 4, 2, 16, 12, 14),
                box(0, 12, 6, 3, 16, 9),
                box(13, 12, 7, 16, 16, 10)
        ), Direction.Axis.Z);
        var shaperDown = VoxelShaper.forHorizontalAxis(Shapes.or(
                box(0, 4, 2, 16, 12, 14),
                box(0, 0, 7, 3, 4, 10),
                box(13, 0, 6, 16, 4, 9)
        ), Direction.Axis.Z);
        terminals = BlockStateTerminalCollection.builder(this)
                .forAllStatesExcept(state -> {
                    var facing = state.getValue(HORIZONTAL_FACING);
                    var up = state.getValue(UP);
                    return BlockStateTerminalCollection.each(TERMINALS_HORIZONTAL, terminal -> terminal
                            .rotateAroundX(up ? 0 : 180)
                            .rotateAroundY((int) facing.toYRot() - 180));
                })
                .withShapeMapper(state -> {
                    var axis = state.getValue(HORIZONTAL_FACING).getAxis();
                    var up = state.getValue(UP);
                    return (up ? shaperUp : shaperDown).get(axis);
                })
                .build();
        terminalsFlipped = BlockStateTerminalCollection.builder(this)
                .forAllStatesExcept(state -> {
                    var facing = state.getValue(HORIZONTAL_FACING);
                    var up = state.getValue(UP);
                    return BlockStateTerminalCollection.each(TERMINALS_HORIZONTAL_FLIPPED, terminal -> terminal
                            .rotateAroundX(up ? 0 : 180)
                            .rotateAroundY((int) facing.toYRot() - 180));
                })
                .withShapeMapper(state -> {
                    var axis = state.getValue(HORIZONTAL_FACING).getAxis();
                    var up = state.getValue(UP);
                    return (up ? shaperUp : shaperDown).get(axis);
                })
                .build();
        outlines = getShapeForEachState(terminals.shapeMapper());
    }

    @Override
    public float getInertia() {
        return ModdedConfigs.server().kinetics.generatorControls.generatorCommutatorInertia.getF();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING, UP);
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
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var result = super.onWrenched(state, context);
        if(result == InteractionResult.SUCCESS && !context.getLevel().isClientSide)
            ElectricBlock.refreshConnectionEntities(context.getLevel(), context.getClickedPos());
        return result;
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        if(targetedFace.getAxis() == Direction.Axis.Y) {
            return super.getRotatedBlockState(originalState, targetedFace);
        }
        return originalState.cycle(UP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    @Override
    public boolean canConnect(BlockState state, Direction dir) {
        return dir.getAxis() == Direction.Axis.Y;
    }

    @Override
    public @NotNull Direction.Axis getAssemblyRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public Vec3 brushOffset(BlockState state) {
        var facing = state.getValue(HORIZONTAL_FACING);
        if(!state.getValue(UP))
            facing = facing.getOpposite();
        return new Vec3(-3.5 / 16, 1 / 16f, 0)
                .yRot((float) Math.PI * (facing.toYRot() - 180) / 180f);
    }

    @Override
    public Vec3 sparkVelocity(BlockState state, float angularVelocity) {
        var facing = state.getValue(HORIZONTAL_FACING);
        if(!state.getValue(UP))
            facing = facing.getOpposite();
        return new Vec3(0, 0, 1 / 4f + Math.abs(angularVelocity) / 100f)
                .yRot((float) Math.PI * (facing.toYRot() - 180) / 180f);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(HORIZONTAL_FACING, rot.rotate(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    public BlockStateTerminalCollection terminals() {
        return terminals;
    }

    @Override
    public BlockStateTerminalCollection terminalsFlipped() {
        return terminalsFlipped;
    }
}
