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
package org.patryk3211.powergrid.electricity.light.fixture;

import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.fabric.EnvExecutor;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.fabricmc.api.EnvType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;

public class LightFixtureBlock extends ElectricBlock implements IBE<LightFixtureBlockEntity> {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final IntProperty POWER = IntProperty.of("power", 0, 2);

    private static final TerminalBoundingBox UP_TERMINAL_1 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 3, 0, 7, 5, 3, 9);
    private static final TerminalBoundingBox UP_TERMINAL_2 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11, 0, 7, 13, 3, 9);

    private static final TerminalBoundingBox DOWN_TERMINAL_1 = UP_TERMINAL_1.rotateAroundX(BlockRotation.CLOCKWISE_180);
    private static final TerminalBoundingBox DOWN_TERMINAL_2 = UP_TERMINAL_2.rotateAroundX(BlockRotation.CLOCKWISE_180);

    private static final TerminalBoundingBox NORTH_TERMINAL_1 = UP_TERMINAL_1.rotateAroundX(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox NORTH_TERMINAL_2 = UP_TERMINAL_2.rotateAroundX(BlockRotation.CLOCKWISE_90);

    private static final TerminalBoundingBox SOUTH_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_180);
    private static final TerminalBoundingBox SOUTH_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_180);

    private static final TerminalBoundingBox EAST_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox EAST_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_90);

    private static final TerminalBoundingBox WEST_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.COUNTERCLOCKWISE_90);
    private static final TerminalBoundingBox WEST_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.COUNTERCLOCKWISE_90);

    private static final VoxelShape SHAPE_UP = VoxelShapes.union(
            createCuboidShape(3.5, 0, 3.5, 12.5, 4, 12.5),
            UP_TERMINAL_1.getShape(),
            UP_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_DOWN = VoxelShapes.union(
            createCuboidShape(3.5, 12, 3.5, 12.5, 16, 12.5),
            DOWN_TERMINAL_1.getShape(),
            DOWN_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_SOUTH = VoxelShapes.union(
            createCuboidShape(3.5, 3.5, 0, 12.5, 12.5, 4),
            SOUTH_TERMINAL_1.getShape(),
            SOUTH_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_NORTH = VoxelShapes.union(
            createCuboidShape(3.5, 3.5, 12, 12.5, 12.5, 16),
            NORTH_TERMINAL_1.getShape(),
            NORTH_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_EAST = VoxelShapes.union(
            createCuboidShape(0, 3.5, 3.5, 4, 12.5, 12.5),
            EAST_TERMINAL_1.getShape(),
            EAST_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_WEST = VoxelShapes.union(
            createCuboidShape(12, 3.5, 3.5, 16, 12.5, 12.5),
            WEST_TERMINAL_1.getShape(),
            WEST_TERMINAL_2.getShape()
    );

    Vec3d modelOffset;

    public LightFixtureBlock(Settings settings) {
        super(settings.luminance(state -> switch(state.get(POWER)) {
            case 1 -> 12;
            case 2 -> 15;
            default -> 0;
        }));
        modelOffset = Vec3d.ZERO;
        setDefaultState(getDefaultState().with(POWER, 0));
    }

    public static <B extends LightFixtureBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> setBulbModelOffset(Vec3d modelOffset) {
        return b -> {
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> b.onRegister(block -> block.modelOffset = modelOffset));
            return b;
        };
    }

    public static <B extends LightFixtureBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> setBulbModelOffset(float x, float y, float z) {
        return b -> {
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> b.onRegister(block -> block.modelOffset = new Vec3d(x, y, z)));
            return b;
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, POWER);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState()
                .with(FACING, ctx.getSide());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(FACING)) {
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
        };
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return switch(state.get(FACING)) {
            case UP -> switch(index) {
                case 0 -> UP_TERMINAL_1;
                case 1 -> UP_TERMINAL_2;
                default -> null;
            };
            case DOWN -> switch(index) {
                case 0 -> DOWN_TERMINAL_1;
                case 1 -> DOWN_TERMINAL_2;
                default -> null;
            };
            case SOUTH -> switch(index) {
                case 0 -> SOUTH_TERMINAL_1;
                case 1 -> SOUTH_TERMINAL_2;
                default -> null;
            };
            case NORTH -> switch(index) {
                case 0 -> NORTH_TERMINAL_1;
                case 1 -> NORTH_TERMINAL_2;
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
        };
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var stack = player.getStackInHand(hand);
        var be = world.getBlockEntity(pos, ModdedBlockEntities.LIGHT_FIXTURE.get());
        if(be.isEmpty())
            return ActionResult.PASS;

        if(stack == null || stack.isEmpty() || stack.getItem() instanceof ILightBulb) {
            return be.get().replaceBulb(player, hand, stack) ? ActionResult.SUCCESS : ActionResult.FAIL;
        } else {
            // Holding something else.
            return ActionResult.PASS;
        }
    }

    @Override
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
        super.onBroken(world, pos, state);
    }

    @Override
    public Class<LightFixtureBlockEntity> getBlockEntityClass() {
        return LightFixtureBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LightFixtureBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.LIGHT_FIXTURE.get();
    }
}
