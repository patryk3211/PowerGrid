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
package org.patryk3211.powergrid.electricity.transformer;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.*;

import java.util.Optional;

public class TransformerSmallBlock extends TransformerBlock implements IBE<TransformerSmallBlockEntity> {
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = Properties.HORIZONTAL_AXIS;

    private static final TerminalBoundingBox Z_TERMINAL_1 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 0, 12, 2, 4, 17, 5);
    private static final TerminalBoundingBox Z_TERMINAL_2 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 0, 12, 11, 4, 17, 14);
    private static final TerminalBoundingBox Z_TERMINAL_3 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11, 12, 2, 15, 17, 5);
    private static final TerminalBoundingBox Z_TERMINAL_4 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11, 12, 11, 15, 17, 14);

    private static final TerminalBoundingBox X_TERMINAL_1 = Z_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox X_TERMINAL_2 = Z_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox X_TERMINAL_3 = Z_TERMINAL_3.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox X_TERMINAL_4 = Z_TERMINAL_4.rotateAroundY(BlockRotation.CLOCKWISE_90);

    private static final VoxelShape SHAPE_Z = VoxelShapes.union(
            createCuboidShape(2, 0, 0, 14, 14, 16),
            Z_TERMINAL_1.getShape(),
            Z_TERMINAL_2.getShape(),
            Z_TERMINAL_3.getShape(),
            Z_TERMINAL_4.getShape()
    );

    private static final VoxelShape SHAPE_X = VoxelShapes.union(
            createCuboidShape(0, 0, 2, 16, 14, 14),
            X_TERMINAL_1.getShape(),
            X_TERMINAL_2.getShape(),
            X_TERMINAL_3.getShape(),
            X_TERMINAL_4.getShape()
    );

    public TransformerSmallBlock(Settings settings) {
        super(settings, 60);
        setDefaultState(getDefaultState().with(COILS, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HORIZONTAL_AXIS, COILS);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(HORIZONTAL_AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public Optional<TransformerBlockEntity> getBlockEntity(World world, BlockPos pos, BlockState state) {
        return Optional.ofNullable(getBlockEntity(world, pos));
    }

    @Override
    protected boolean isInitiator(BlockPos pos, BlockState state, BlockPos initiator) {
        return pos.equals(initiator);
    }

    @Override
    public int terminalCount() {
        return 4;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return switch(state.get(HORIZONTAL_AXIS)) {
            case X -> switch(index) {
                case 0 -> X_TERMINAL_1;
                case 1 -> X_TERMINAL_2;
                case 2 -> X_TERMINAL_3;
                case 3 -> X_TERMINAL_4;
                default -> null;
            };
            case Z -> switch(index) {
                case 0 -> Z_TERMINAL_1;
                case 1 -> Z_TERMINAL_2;
                case 2 -> Z_TERMINAL_3;
                case 3 -> Z_TERMINAL_4;
                default -> null;
            };
            default -> null;
        };
    }

    @Override
    public Class<TransformerSmallBlockEntity> getBlockEntityClass() {
        return TransformerSmallBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TransformerSmallBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.TRANSFORMER_SMALL.get();
    }
}
