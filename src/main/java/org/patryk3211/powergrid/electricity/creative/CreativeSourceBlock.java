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
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeSourceBlock extends ElectricBlock implements IBE<CreativeSourceBlockEntity> {
    public static final Property<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final TerminalBoundingBox Z_TERMINAL_1 =
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 6, 13, 2, 10, 16, 6)
                    .withColor(IDecoratedTerminal.RED);
    private static final TerminalBoundingBox Z_TERMINAL_2 =
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 6, 13, 10, 10, 16, 14)
                    .withColor(IDecoratedTerminal.BLUE);

    private static final TerminalBoundingBox X_TERMINAL_1 = Z_TERMINAL_1.rotateAroundY(Rotation.CLOCKWISE_90);
    private static final TerminalBoundingBox X_TERMINAL_2 = Z_TERMINAL_2.rotateAroundY(Rotation.CLOCKWISE_90);

    private static final VoxelShape SHAPE_X = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(1, 2, 1, 15, 13, 15),
            X_TERMINAL_1.getShape(),
            X_TERMINAL_2.getShape()
    );
    private static final VoxelShape SHAPE_Z = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(1, 2, 1, 15, 13, 15),
            Z_TERMINAL_1.getShape(),
            Z_TERMINAL_2.getShape()
    );

    public CreativeSourceBlock(Properties settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(HORIZONTAL_AXIS, ctx.getHorizontalDirection().getClockWise().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch(state.getValue(HORIZONTAL_AXIS)) {
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
        return switch(state.getValue(HORIZONTAL_AXIS)) {
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
