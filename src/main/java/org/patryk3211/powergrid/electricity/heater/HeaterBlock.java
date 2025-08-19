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
package org.patryk3211.powergrid.electricity.heater;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;

public class HeaterBlock extends ElectricBlock implements IBE<HeaterBlockEntity>, IHaveElectricProperties {
    private static final TerminalBoundingBox NORTH_TERMINAL1 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 12, 12, 7, 15, 15, 10);
    private static final TerminalBoundingBox NORTH_TERMINAL2 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 1, 12, 7, 4, 15, 10);

    private static final TerminalBoundingBox SOUTH_TERMINAL1 = NORTH_TERMINAL1.rotateAroundY(Rotation.CLOCKWISE_180);
    private static final TerminalBoundingBox SOUTH_TERMINAL2 = NORTH_TERMINAL2.rotateAroundY(Rotation.CLOCKWISE_180);

    private static final TerminalBoundingBox EAST_TERMINAL1 = NORTH_TERMINAL1.rotateAroundY(Rotation.CLOCKWISE_90);
    private static final TerminalBoundingBox EAST_TERMINAL2 = NORTH_TERMINAL2.rotateAroundY(Rotation.CLOCKWISE_90);

    private static final TerminalBoundingBox WEST_TERMINAL1 = NORTH_TERMINAL1.rotateAroundY(Rotation.COUNTERCLOCKWISE_90);
    private static final TerminalBoundingBox WEST_TERMINAL2 = NORTH_TERMINAL2.rotateAroundY(Rotation.COUNTERCLOCKWISE_90);

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            box(0, 0, 5, 16, 12, 11),
            NORTH_TERMINAL1.getShape(),
            NORTH_TERMINAL2.getShape()
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            box(0, 0, 5, 16, 12, 11),
            SOUTH_TERMINAL1.getShape(),
            SOUTH_TERMINAL2.getShape()
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
            box(5, 0, 0, 11, 12, 16),
            EAST_TERMINAL1.getShape(),
            EAST_TERMINAL2.getShape()
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
            box(5, 0, 0, 11, 12, 16),
            WEST_TERMINAL1.getShape(),
            WEST_TERMINAL2.getShape()
    );

    public HeaterBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return switch(state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case NORTH -> switch(index) {
                case 0 -> NORTH_TERMINAL1;
                case 1 -> NORTH_TERMINAL2;
                default -> null;
            };
            case SOUTH -> switch(index) {
                case 0 -> SOUTH_TERMINAL1;
                case 1 -> SOUTH_TERMINAL2;
                default -> null;
            };
            case EAST -> switch(index) {
                case 0 -> EAST_TERMINAL1;
                case 1 -> EAST_TERMINAL2;
                default -> null;
            };
            case WEST -> switch(index) {
                case 0 -> WEST_TERMINAL1;
                case 1 -> WEST_TERMINAL2;
                default -> null;
            };
            default -> null;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch(state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> throw new IllegalArgumentException("Invalid horizontal facing");
        };
    }

    @Override
    public Class<HeaterBlockEntity> getBlockEntityClass() {
        return HeaterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeaterBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.HEATING_COIL.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(resistance(), player, tooltip);
        Power.max(stack, player, tooltip);
    }
}
