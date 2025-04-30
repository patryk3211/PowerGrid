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
package org.patryk3211.powergrid.electricity.creative;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class CreativeSourceBlock extends ElectricBlock implements IBE<CreativeSourceBlockEntity> {
    public static final Property<Direction.Axis> HORIZONTAL_AXIS = Properties.HORIZONTAL_AXIS;

    private static final TerminalBoundingBox Z_TERMINAL_1 =
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 6, 13, 2, 10, 16, 6)
                    .withColor(IDecoratedTerminal.RED);
    private static final TerminalBoundingBox Z_TERMINAL_2 =
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 6, 13, 10, 10, 16, 14)
                    .withColor(IDecoratedTerminal.BLUE);

    private static final TerminalBoundingBox X_TERMINAL_1 = Z_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox X_TERMINAL_2 = Z_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_90);

    private static final VoxelShape SHAPE_X = VoxelShapes.union(
            createCuboidShape(0, 0, 0, 16, 2, 16),
            createCuboidShape(1, 2, 1, 15, 13, 15),
            X_TERMINAL_1.getShape(),
            X_TERMINAL_2.getShape()
    );
    private static final VoxelShape SHAPE_Z = VoxelShapes.union(
            createCuboidShape(0, 0, 0, 16, 2, 16),
            createCuboidShape(1, 2, 1, 15, 13, 15),
            Z_TERMINAL_1.getShape(),
            Z_TERMINAL_2.getShape()
    );

    public CreativeSourceBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(HORIZONTAL_AXIS, ctx.getHorizontalPlayerFacing().rotateYClockwise().getAxis());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HORIZONTAL_AXIS);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(HORIZONTAL_AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            case Y -> throw new IllegalStateException();
        };
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return switch(state.get(HORIZONTAL_AXIS)) {
            case X -> switch(index) {
                case 0 -> X_TERMINAL_1;
                case 1-> X_TERMINAL_2;
                default -> null;
            };
            case Z -> switch(index) {
                case 0 -> Z_TERMINAL_1;
                case 1 -> Z_TERMINAL_2;
                default -> null;
            };
            case Y -> null;
        };
    }

    @Override
    public Class<CreativeSourceBlockEntity> getBlockEntityClass() {
        return CreativeSourceBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CreativeSourceBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CREATIVE_SOURCE.get();
    }
}
