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
package org.patryk3211.powergrid.kinetics.basicgenerator;

import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.PowerGridRegistrate;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.INamedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;

public class BasicGeneratorBlock extends HorizontalKineticBlock implements IBE<BasicGeneratorBlockEntity>, IElectric, IHaveElectricProperties {
    private static final TerminalBoundingBox NORTH_TERMINAL_1 =
            new TerminalBoundingBox(INamedTerminal.POSITIVE, 3, 12, 13, 5, 16, 16, 0.5)
                    .withOrigin(4, 15, 14.5);
    private static final TerminalBoundingBox NORTH_TERMINAL_2 =
            new TerminalBoundingBox(INamedTerminal.NEGATIVE, 11, 12, 13, 13, 16, 16, 0.5)
                    .withOrigin(12, 15, 14.5);

    private static final TerminalBoundingBox SOUTH_TERMINAL_1 = NORTH_TERMINAL_1.rotated(Direction.SOUTH);
    private static final TerminalBoundingBox SOUTH_TERMINAL_2 = NORTH_TERMINAL_2.rotated(Direction.SOUTH);

    private static final TerminalBoundingBox EAST_TERMINAL_1 = NORTH_TERMINAL_1.rotated(Direction.EAST);
    private static final TerminalBoundingBox EAST_TERMINAL_2 = NORTH_TERMINAL_2.rotated(Direction.EAST);

    private static final TerminalBoundingBox WEST_TERMINAL_1 = NORTH_TERMINAL_1.rotated(Direction.WEST);
    private static final TerminalBoundingBox WEST_TERMINAL_2 = NORTH_TERMINAL_2.rotated(Direction.WEST);

    private static final VoxelShape SHAPE_NORTH = VoxelShapes.union(
            createCuboidShape(1, 0, 0, 15, 2, 16),
            createCuboidShape(2, 2, 1, 14, 14, 15),
            NORTH_TERMINAL_1.getShape(),
            NORTH_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_SOUTH = VoxelShapes.union(
            createCuboidShape(1, 0, 0, 15, 2, 16),
            createCuboidShape(2, 2, 1, 14, 14, 15),
            SOUTH_TERMINAL_1.getShape(),
            SOUTH_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_EAST = VoxelShapes.union(
            createCuboidShape(0, 0, 1, 16, 2, 15),
            createCuboidShape(1, 2, 2, 15, 14, 14),
            EAST_TERMINAL_1.getShape(),
            EAST_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_WEST = VoxelShapes.union(
            createCuboidShape(0, 0, 1, 16, 2, 15),
            createCuboidShape(1, 2, 2, 15, 14, 14),
            WEST_TERMINAL_1.getShape(),
            WEST_TERMINAL_2.getShape()
    );

    public BasicGeneratorBlock(Settings settings) {
        super(settings);
    }

    public static BlockEntry<BasicGeneratorBlock> register(final PowerGridRegistrate registrate) {
        return registrate.block("basic_generator", BasicGeneratorBlock::new)
                .transform(BlockStressDefaults.setImpact(4.0))
                .simpleItem()
                .register();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState blockState) {
        return blockState.get(Properties.HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
        return state.get(Properties.HORIZONTAL_FACING) == face;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        Direction preferred = this.getPreferredHorizontalFacing(context);
        return (context.getPlayer() == null || !context.getPlayer().isSneaking()) && preferred != null
                ? this.getDefaultState().with(Properties.HORIZONTAL_FACING, preferred)
                : super.getPlacementState(context);
    }

    @Override
    public Class<BasicGeneratorBlockEntity> getBlockEntityClass() {
        return BasicGeneratorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BasicGeneratorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.BASIC_GENERATOR.get();
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(Properties.HORIZONTAL_FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> super.getOutlineShape(state, world, pos, context);
        };
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return switch(state.get(Properties.HORIZONTAL_FACING)) {
            case NORTH -> switch(index) {
                case 0 -> NORTH_TERMINAL_1;
                case 1 -> NORTH_TERMINAL_2;
                default -> null;
            };
            case SOUTH -> switch(index) {
                case 0 -> SOUTH_TERMINAL_1;
                case 1 -> SOUTH_TERMINAL_2;
                default -> null;
            };
            case EAST -> switch(index) {
                case 0 -> EAST_TERMINAL_1;
                case 1 -> EAST_TERMINAL_2;
                default -> null;
            };
            case WEST -> switch(index) {
                case 0 -> WEST_TERMINAL_1;
                case 1 -> WEST_TERMINAL_2;
                default -> null;
            };
            default -> null;
        };
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        Resistance.series(resistance(), player, tooltip);
    }

    public static float resistance() {
        return ModdedConfigs.server().kinetics.basicGeneratorResistance.getF();
    }
}
