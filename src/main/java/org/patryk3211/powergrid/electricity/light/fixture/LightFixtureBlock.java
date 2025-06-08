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
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
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
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;

public class LightFixtureBlock extends ElectricBlock implements IBE<LightFixtureBlockEntity> {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final IntProperty POWER = IntProperty.of("power", 0, 2);
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;

    private static final TerminalBoundingBox[] UP_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 3, 0, 7, 5, 3, 9),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11, 0, 7, 13, 3, 9)
    };

    private static final VoxelShape SHAPE_UP = createCuboidShape(3.5, 0, 3.5, 12.5, 4, 12.5);
    private static final VoxelShape SHAPE_DOWN = createCuboidShape(3.5, 12, 3.5, 12.5, 16, 12.5);
    private static final VoxelShape SHAPE_SOUTH = createCuboidShape(3.5, 3.5, 0, 12.5, 12.5, 4);
    private static final VoxelShape SHAPE_NORTH = createCuboidShape(3.5, 3.5, 12, 12.5, 12.5, 16);
    private static final VoxelShape SHAPE_EAST = createCuboidShape(0, 3.5, 3.5, 4, 12.5, 12.5);
    private static final VoxelShape SHAPE_WEST = createCuboidShape(12, 3.5, 3.5, 16, 12.5, 12.5);

    Vec3d modelOffset;

    public LightFixtureBlock(Settings settings) {
        super(settings.luminance(state -> switch(state.get(POWER)) {
            case 1 -> 10;
            case 2 -> 15;
            default -> 0;
        }));
        modelOffset = Vec3d.ZERO;
        setDefaultState(getDefaultState().with(POWER, 0));

        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStatesExcept(state -> BlockStateTerminalCollection.each(UP_TERMINALS, terminal -> {
                    var facing = state.get(FACING);
                    terminal = switch(facing) {
                        case UP -> terminal;
                        case DOWN -> terminal.rotateAroundX(BlockRotation.CLOCKWISE_180);
                        case NORTH -> terminal.rotateAroundX(BlockRotation.CLOCKWISE_90);
                        case SOUTH -> terminal.rotateAroundX(BlockRotation.COUNTERCLOCKWISE_90);
                        case EAST -> terminal.rotateAroundX(BlockRotation.CLOCKWISE_90).rotateAroundY(BlockRotation.CLOCKWISE_90);
                        case WEST -> terminal.rotateAroundX(BlockRotation.CLOCKWISE_90).rotateAroundY(BlockRotation.COUNTERCLOCKWISE_90);
                    };
                    if(!state.get(ALONG_FIRST_AXIS)) {
                        terminal = terminal.rotate(facing.getAxis(), BlockRotation.CLOCKWISE_90);
                    }
                    return terminal;
                }), POWER)
                .withShapeMapper(state -> switch(state.get(FACING)) {
                    case UP -> SHAPE_UP;
                    case DOWN -> SHAPE_DOWN;
                    case EAST -> SHAPE_EAST;
                    case WEST -> SHAPE_WEST;
                    case NORTH -> SHAPE_NORTH;
                    case SOUTH -> SHAPE_SOUTH;
                }).build());
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
        builder.add(FACING, POWER, ALONG_FIRST_AXIS);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var facing = ctx.getSide();
        boolean along = true;
        if(facing.getAxis() == Direction.Axis.Y) {
            var player = ctx.getHorizontalPlayerFacing();
            if(player.getAxis() == Direction.Axis.X)
                along = false;
        }

        return getDefaultState()
                .with(FACING, facing)
                .with(ALONG_FIRST_AXIS, along);
    }

    @Override
    public int terminalCount() {
        return 2;
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
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.fullCube();
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
