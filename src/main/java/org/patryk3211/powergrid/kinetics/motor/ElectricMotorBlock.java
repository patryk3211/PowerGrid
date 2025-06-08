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
package org.patryk3211.powergrid.kinetics.motor;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.RotatedTerminalCollection;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.utility.Directions;

import java.util.List;

public class ElectricMotorBlock extends DirectionalKineticBlock implements IElectric, IBE<ElectricMotorBlockEntity>, IHaveElectricProperties {
    private static final RotatedTerminalCollection TERMINALS = RotatedTerminalCollection
            .builder(RotatedTerminalCollection::rotateNorthToFacing)
            .add(new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 0, 7, 13, 3, 9, 16))
            .add(new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 13, 7, 13, 16, 9, 16))
            .with(Directions.ALL)
            .build();

    private static final VoxelShape SHAPE_NORTH = VoxelShapes.union(
            createCuboidShape(2, 2, 2, 14, 14, 15),
            TERMINALS.get(Direction.NORTH, 0).getShape(),
            TERMINALS.get(Direction.NORTH, 1).getShape()
    );

    private static final VoxelShape SHAPE_SOUTH = VoxelShapes.union(
            createCuboidShape(2, 2, 1, 14, 14, 14),
            TERMINALS.get(Direction.SOUTH, 0).getShape(),
            TERMINALS.get(Direction.SOUTH, 1).getShape()
    );

    private static final VoxelShape SHAPE_WEST = VoxelShapes.union(
            createCuboidShape(2, 2, 2, 15, 14, 14),
            TERMINALS.get(Direction.WEST, 0).getShape(),
            TERMINALS.get(Direction.WEST, 1).getShape()
    );

    private static final VoxelShape SHAPE_EAST = VoxelShapes.union(
            createCuboidShape(1, 2, 2, 14, 14, 14),
            TERMINALS.get(Direction.EAST, 0).getShape(),
            TERMINALS.get(Direction.EAST, 1).getShape()
    );

    private static final VoxelShape SHAPE_DOWN = VoxelShapes.union(
            createCuboidShape(2, 2, 2, 14, 15, 14),
            TERMINALS.get(Direction.DOWN, 0).getShape(),
            TERMINALS.get(Direction.DOWN, 1).getShape()
    );

    private static final VoxelShape SHAPE_UP = VoxelShapes.union(
            createCuboidShape(2, 1, 2, 14, 14, 14),
            TERMINALS.get(Direction.UP, 0).getShape(),
            TERMINALS.get(Direction.UP, 1).getShape()
    );

    public ElectricMotorBlock(Settings properties) {
        super(properties);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(FACING)) {
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            case SOUTH -> SHAPE_SOUTH;
            case NORTH -> SHAPE_NORTH;
        };
    }

    @Override
    public Direction getPreferredFacing(ItemPlacementContext context) {
        var facing = super.getPreferredFacing(context);
        return facing == null ? null : facing.getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.get(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
        return face == state.get(FACING);
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return TERMINALS.get(state.get(FACING), index);
    }

    @Override
    public Class<ElectricMotorBlockEntity> getBlockEntityClass() {
        return ElectricMotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ElectricMotorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.ELECTRIC_MOTOR.get();
    }

    public static float resistance() {
        return 10f;
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        Resistance.series(resistance(), player, tooltip);
    }
}
