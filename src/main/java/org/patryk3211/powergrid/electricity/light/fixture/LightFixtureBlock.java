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
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;

public class LightFixtureBlock extends ElectricBlock implements IBE<LightFixtureBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 2);
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;

    private static final TerminalBoundingBox[] UP_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 3, 0, 7, 5, 3, 9),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11, 0, 7, 13, 3, 9)
    };

    private static final VoxelShape SHAPE_UP = box(3.5, 0, 3.5, 12.5, 4, 12.5);
    private static final VoxelShape SHAPE_DOWN = box(3.5, 12, 3.5, 12.5, 16, 12.5);
    private static final VoxelShape SHAPE_SOUTH = box(3.5, 3.5, 0, 12.5, 12.5, 4);
    private static final VoxelShape SHAPE_NORTH = box(3.5, 3.5, 12, 12.5, 12.5, 16);
    private static final VoxelShape SHAPE_EAST = box(0, 3.5, 3.5, 4, 12.5, 12.5);
    private static final VoxelShape SHAPE_WEST = box(12, 3.5, 3.5, 16, 12.5, 12.5);

    Vec3 modelOffset;

    public LightFixtureBlock(Properties settings) {
        super(settings.lightLevel(state -> switch(state.getValue(POWER)) {
            case 1 -> 10;
            case 2 -> 15;
            default -> 0;
        }));
        modelOffset = Vec3.ZERO;
        registerDefaultState(defaultBlockState().setValue(POWER, 0));

        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStatesExcept(state -> BlockStateTerminalCollection.each(UP_TERMINALS, terminal -> {
                    var facing = state.getValue(FACING);
                    terminal = switch(facing) {
                        case UP -> terminal;
                        case DOWN -> terminal.rotateAroundX(Rotation.CLOCKWISE_180);
                        case NORTH -> terminal.rotateAroundX(Rotation.CLOCKWISE_90);
                        case SOUTH -> terminal.rotateAroundX(Rotation.COUNTERCLOCKWISE_90);
                        case EAST -> terminal.rotateAroundX(Rotation.CLOCKWISE_90).rotateAroundY(Rotation.CLOCKWISE_90);
                        case WEST -> terminal.rotateAroundX(Rotation.CLOCKWISE_90).rotateAroundY(Rotation.COUNTERCLOCKWISE_90);
                    };
                    if(!state.getValue(ALONG_FIRST_AXIS)) {
                        terminal = terminal.rotate(facing.getAxis(), Rotation.CLOCKWISE_90);
                    }
                    return terminal;
                }), POWER)
                .withShapeMapper(state -> switch(state.getValue(FACING)) {
                    case UP -> SHAPE_UP;
                    case DOWN -> SHAPE_DOWN;
                    case EAST -> SHAPE_EAST;
                    case WEST -> SHAPE_WEST;
                    case NORTH -> SHAPE_NORTH;
                    case SOUTH -> SHAPE_SOUTH;
                }).build());
    }

    public static <B extends LightFixtureBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> setBulbModelOffset(Vec3 modelOffset) {
        return b -> {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> b.onRegister(block -> block.modelOffset = modelOffset));
            return b;
        };
    }

    public static <B extends LightFixtureBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> setBulbModelOffset(float x, float y, float z) {
        return b -> {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> b.onRegister(block -> block.modelOffset = new Vec3(x, y, z)));
            return b;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWER, ALONG_FIRST_AXIS);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        var facing = state.getValue(FACING);
        return canSupportCenter(world, pos.relative(facing, -1), facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        var facing = state.getValue(FACING);
        return direction == facing.getOpposite() && !canSurvive(state, world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getClickedFace();
        boolean along = true;
        if(facing.getAxis() == Direction.Axis.Y) {
            var player = ctx.getHorizontalDirection();
            if(player.getAxis() == Direction.Axis.X)
                along = false;
        }

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ALONG_FIRST_AXIS, along);
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var stack = player.getItemInHand(hand);
        var be = world.getBlockEntity(pos, ModdedBlockEntities.LIGHT_FIXTURE.get());
        if(be.isEmpty())
            return InteractionResult.PASS;

        if(stack == null || stack.isEmpty() || stack.getItem() instanceof ILightBulb) {
            return be.get().replaceBulb(player, hand, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        } else {
            // Holding something else.
            return InteractionResult.PASS;
        }
    }

    @Override
    public void destroy(LevelAccessor world, BlockPos pos, BlockState state) {
        super.destroy(world, pos, state);
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
