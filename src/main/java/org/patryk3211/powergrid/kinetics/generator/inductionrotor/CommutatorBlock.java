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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.*;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.kinetics.generator.rotor.AbstractRotorBlock;
import org.patryk3211.powergrid.utility.Directions;

public class CommutatorBlock extends AbstractRotorBlock implements IBE<CommutatorBlockEntity>, IElectric {
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = Properties.HORIZONTAL_AXIS;

    private final BlockStateTerminalCollection terminals;
    private final ImmutableMap<BlockState, VoxelShape> outlines;

    private static final TerminalBoundingBox[] TERMINALS_HORIZONTAL = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 0, 14, 6, 3, 16, 9)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 13, 14, 7, 16, 16, 10)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    public CommutatorBlock(Settings properties) {
        super(properties);
        var baseShaper = VoxelShaper.forHorizontalAxis(VoxelShapes.union(
                createCuboidShape(0, 0, 3, 16, 12, 13),
                createCuboidShape(0, 12, 6, 3, 16, 9),
                createCuboidShape(13, 12, 7, 16, 16, 10)
        ), Direction.Axis.Z);
        terminals = BlockStateTerminalCollection.builder(this)
                .forAllStatesExcept(state -> {
                    var axis = state.get(HORIZONTAL_AXIS);
                    if(axis == Direction.Axis.X)
                        return BlockStateTerminalCollection.each(TERMINALS_HORIZONTAL, terminal ->
                                terminal.rotateAroundY(90));
                    return TERMINALS_HORIZONTAL;
                })
                .withShapeMapper(state -> {
                    var axis = state.get(HORIZONTAL_AXIS);
                    return baseShaper.get(axis);
                })
                .build();
        outlines = getShapesForStates(terminals.shapeMapper());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_AXIS);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
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
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        var result = super.onWrenched(state, context);
        if(result == ActionResult.SUCCESS && !context.getWorld().isClient)
            ElectricBlock.refreshConnectionEntities(context.getWorld(), context.getBlockPos());
        return result;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();

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

        if(preferredAxis == null)
            preferredAxis = context.getHorizontalPlayerFacing().getAxis();

        return getDefaultState()
                .with(HORIZONTAL_AXIS, preferredAxis);
    }

    @Override
    public boolean canConnect(BlockState state, Direction dir) {
        return state.get(HORIZONTAL_AXIS) == dir.getAxis();
    }
}
