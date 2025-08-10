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
package org.patryk3211.powergrid.kinetics.generator.winding;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing;

import java.util.Optional;
import java.util.function.BiConsumer;

import static org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing.HORIZONTAL_FACING;
import static org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing.UP;

public class WindingBlock extends ElectricBlock implements IBE<WindingBlockEntity> {
    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;
    public static final IntProperty PART = IntProperty.of("part", 0, 2);
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;

    public static final BooleanProperty CASE_RIGHT = BooleanProperty.of("right");
    public static final BooleanProperty CASE_LEFT = BooleanProperty.of("left");

    private static final VoxelShaper HORIZONTAL_END_SHAPER = VoxelShaper.forDirectional(VoxelShapes.union(
            createCuboidShape(2, 3, 3, 14, 13, 16),
            createCuboidShape(0, 6, 6, 16, 10, 10),
            createCuboidShape(6, 6, 0, 10, 10, 3)
    ), Direction.SOUTH);
    private static final VoxelShaper VERTICAL_END_SHAPER = VoxelShaper.forDirectional(VoxelShapes.union(
            createCuboidShape(3, 2, 3, 13, 14, 16),
            createCuboidShape(6, 0, 6, 10, 16, 10),
            createCuboidShape(6, 6, 0, 10, 10, 3)
    ), Direction.SOUTH);

    private static final VoxelShaper HORIZONTAL_MIDDLE_SHAPER = VoxelShaper.forAxis(
            createCuboidShape(2, 3, 0, 14, 13, 16),
            Direction.Axis.Z
    );
    private static final VoxelShaper VERTICAL_MIDDLE_SHAPER = VoxelShaper.forAxis(
            createCuboidShape(3, 2, 0, 13, 14, 16),
            Direction.Axis.Z
    );

    private static final TerminalBoundingBox TERMINAL_POSITIVE =
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 6, 6, 0, 10, 10, 2)
                    .withColor(IDecoratedTerminal.RED);
    private static final TerminalBoundingBox TERMINAL_NEGATIVE =
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 6, 6, 14, 10, 10, 16)
                    .withColor(IDecoratedTerminal.BLUE);

    public WindingBlock(Settings settings) {
        super(settings);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStatesExcept(state -> {
                    var terminals = new TerminalBoundingBox[2];
                    var part = state.get(PART);
                    if(part == 1)
                        return terminals;
                    var terminalIn = part == 0 ? TERMINAL_POSITIVE : TERMINAL_NEGATIVE;
                    switch(state.get(AXIS)) {
                        case X -> terminalIn = terminalIn.rotateAroundY(-90);
                        case Y -> terminalIn = terminalIn.rotateAroundX(90);
                    }
                    terminals[part / 2] = terminalIn;
                    return terminals;
                }, ALONG_FIRST_AXIS, CASE_RIGHT, CASE_LEFT)
                .build());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        var part = state.get(PART);
        if(part == 0 || part == 2) {
            var dir = Direction.from(state.get(AXIS), part == 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            var along = state.get(ALONG_FIRST_AXIS);
            var shaper = along ? VERTICAL_END_SHAPER : HORIZONTAL_END_SHAPER;
            return shaper.get(dir);
        } else {
            var along = state.get(ALONG_FIRST_AXIS);
            var shaper = along ? VERTICAL_MIDDLE_SHAPER : HORIZONTAL_MIDDLE_SHAPER;
            return shaper.get(state.get(AXIS));
        }
    }

    private void walkForward(WorldAccess world, BlockPos pos, Direction.Axis axis, BiConsumer<BlockPos, BlockState> callback) {
        BlockState state;
        boolean last = false;
        while(!last) {
            pos = pos.offset(axis, 1);
            state = world.getBlockState(pos);
            if(!state.isOf(this))
                return;
            if(state.get(PART) == 2)
                last = true;
            callback.accept(pos, state);
        }
    }

    public void walkBackward(WorldAccess world, BlockPos pos, Direction.Axis axis, BiConsumer<BlockPos, BlockState> callback) {
        BlockState state;
        boolean last = false;
        while(!last) {
            pos = pos.offset(axis, -1);
            state = world.getBlockState(pos);
            if(!state.isOf(this))
                return;
            if(state.get(PART) == 0)
                last = true;
            callback.accept(pos, state);
        }
    }

    public void walk(WorldAccess world, BlockPos pos, BiConsumer<BlockPos, BlockState> callback) {
        var state = world.getBlockState(pos);
        if(!state.isOf(this))
            return;
        callback.accept(pos, state);
        var axis = state.get(AXIS);
        switch(state.get(PART)) {
            case 0 -> walkForward(world, pos, axis, callback);
            case 1 -> {
                walkForward(world, pos, axis, callback);
                walkBackward(world, pos, axis, callback);
            }
            case 2 -> walkBackward(world, pos, axis, callback);
        }
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.afterBreak(world, player, pos, state, blockEntity, tool);
        if(state.get(PART) != 1) {
            dropStack(world, pos, AllBlocks.SHAFT.asStack());
        }
    }

    @Override
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
        super.onBroken(world, pos, state);
        if(world.isClient())
            return;
        var axis = state.get(AXIS);
        BiConsumer<BlockPos, BlockState> breakBlock = (pos1, state1) -> {
            world.breakBlock(pos1, true);
            if(state1.get(PART) != 1) {
                world.setBlockState(pos1, AllBlocks.SHAFT.getDefaultState().with(AXIS, getMagneticAxis(state1)), NOTIFY_ALL);
            }
        };
        switch(state.get(PART)) {
            case 0 -> walkForward(world, pos, axis, breakBlock);
            case 1 -> {
                walkForward(world, pos, axis, breakBlock);
                walkBackward(world, pos, axis, breakBlock);
            }
            case 2 -> walkBackward(world, pos, axis, breakBlock);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(AXIS, PART, ALONG_FIRST_AXIS, CASE_RIGHT, CASE_LEFT);
    }

    public static boolean canConnect(BlockState thisState, boolean positive, BlockState state) {
        if(state.getBlock() instanceof WindingBlock windingBlock) {
            // Another winding, check for alignment
            if(state.get(AXIS) == thisState.get(AXIS) && state.get(ALONG_FIRST_AXIS) == thisState.get(ALONG_FIRST_AXIS)) {
                // Alignment matches and block entity is valid, these can be connected.
                return true;
            }
        } else if(state.getBlock() instanceof GeneratorHousing) {
            var windingBlock = (WindingBlock) thisState.getBlock();
            var parallelAxis = windingBlock.getParallelCheckAxis(thisState);
            if(parallelAxis.isHorizontal()) {
                var expectedFacing = Direction.from(parallelAxis, positive ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
                return state.get(HORIZONTAL_FACING) == expectedFacing;
            } else {
                var expectUp = !positive;
                return state.get(UP) == expectUp;
            }
        }
        return false;
    }

    public void updateCase(BlockState state, WorldAccess world, BlockPos pos) {
        var axis = getParallelCheckAxis(state);
        var stateN = world.getBlockState(pos.offset(axis, -1));
        var left = canConnect(state, false, stateN);

        var stateP = world.getBlockState(pos.offset(axis, 1));
        var right = canConnect(state, true, stateP);

        var newState = state.with(CASE_LEFT, left)
                .with(CASE_RIGHT, right);
        if(newState != state) {
            world.setBlockState(pos, newState, 0);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        updateCase(state, world, pos);

        var dir = sourcePos.subtract(pos);
        if(Direction.fromVector(dir.getX(), dir.getY(), dir.getZ()).getAxis() == state.get(AXIS))
            return;

        withBlockEntityDo(world, pos, be -> be.onNeighborChanged(sourcePos));
    }

    @Override
    public void prepare(BlockState state, WorldAccess world, BlockPos pos, int flags, int maxUpdateDepth) {
        super.prepare(state, world, pos, flags, maxUpdateDepth);
        updateCase(state, world, pos);
    }

    @Nullable
    public BlockPos getMainBlockPos(World world, BlockPos pos) {
        var state = world.getBlockState(pos);
        if(!state.isOf(this))
            return null;
        var axis = state.get(AXIS);
        switch(state.get(PART)) {
            case 0 -> {
                return pos;
            }
            case 1, 2 -> {
                while(true) {
                    pos = pos.offset(axis, -1);
                    state = world.getBlockState(pos);
                    if(!state.isOf(this))
                        return null;
                    if(state.get(PART) == 0)
                        return pos;
                }
            }
            default -> {
                return null;
            }
        }
    }

    public Optional<WindingBlockEntity> getMainBlockEntity(World world, BlockPos pos) {
        var mainPos = getMainBlockPos(world, pos);
        if(mainPos != null)
            return world.getBlockEntity(mainPos, ModdedBlockEntities.WINDING.get());
        return Optional.empty();
    }

    @Override
    public ElectricBehaviour getBehaviour(World world, BlockPos pos, BlockState state) {
        return getMainBlockEntity(world, pos)
                .map(winding -> winding.getBehaviourProvider().getBehaviour(ElectricBehaviour.TYPE))
                .orElse(null);
    }

    public Direction.Axis getParallelCheckAxis(BlockState state) {
        var along = state.get(ALONG_FIRST_AXIS);
        return switch(state.get(AXIS)) {
            case X -> along ? Direction.Axis.Z : Direction.Axis.Y;
            case Y -> along ? Direction.Axis.X : Direction.Axis.Z;
            case Z -> along ? Direction.Axis.X : Direction.Axis.Y;
        };
    }

    public Direction.Axis getMagneticAxis(BlockState state) {
        var along = state.get(ALONG_FIRST_AXIS);
        return switch(state.get(AXIS)) {
            case X -> along ? Direction.Axis.Y : Direction.Axis.Z;
            case Y -> along ? Direction.Axis.Z : Direction.Axis.X;
            case Z -> along ? Direction.Axis.Y : Direction.Axis.X;
        };
    }

    @Override
    public Class<WindingBlockEntity> getBlockEntityClass() {
        return WindingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WindingBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.WINDING.get();
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        var world = context.getWorld();
        var pos = context.getBlockPos();
        var player = context.getPlayer();

        var part = state.get(PART);
        var axis = state.get(AXIS);
        switch(part) {
            case 0 -> {
                if(world.getBlockState(pos.offset(axis, 1)).get(PART) != 1)
                    return ActionResult.FAIL;
            }
            case 1 -> {
                return ActionResult.FAIL;
            }
            case 2 -> {
                if(world.getBlockState(pos.offset(axis, -1)).get(PART) != 1)
                    return ActionResult.FAIL;
            }
        }

        boolean shouldBreak = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, world.getBlockState(pos), null);
        if(!shouldBreak)
            return ActionResult.SUCCESS;

        if(!(world instanceof ServerWorld serverWorld))
            return ActionResult.SUCCESS;

        if(player != null && !player.isCreative()) {
            Block.getDroppedStacks(state.with(PART, 1), serverWorld, pos, world.getBlockEntity(pos), player, context.getStack())
                    .forEach(stack -> player.getInventory().offerOrDrop(stack));
        }
        state.with(PART, 1).onStacksDropped(serverWorld, pos, ItemStack.EMPTY, true);
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
        var newPos = pos.offset(axis, 1 - part);
        world.setBlockState(newPos, state);
        if(state.get(PART) == 0)
            withBlockEntityDo(world, newPos, WindingBlockEntity::makeMain);

        playRemoveSound(world, pos);
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
        var world = context.getWorld();
        var pos = context.getBlockPos();
        var player = context.getPlayer();

        if(!(world instanceof ServerWorld serverLevel))
            return ActionResult.SUCCESS;

        boolean shouldBreak = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, world.getBlockState(pos), null);
        if(!shouldBreak)
            return ActionResult.SUCCESS;

        walk(world, pos, (pos1, state1) -> {
            if (player != null && !player.isCreative()) {
                Block.getDroppedStacks(state1, serverLevel, pos1, world.getBlockEntity(pos1), player, context.getStack())
                        .forEach(stack -> player.getInventory().offerOrDrop(stack));
                if(pos.equals(pos1) && state1.get(PART) != 1) {
                    player.getInventory().offerOrDrop(AllBlocks.SHAFT.asStack());
                }
            }
            state1.onStacksDropped(serverLevel, pos1, ItemStack.EMPTY, true);
            world.breakBlock(pos1, false);
            if(!pos.equals(pos1) && state1.get(PART) != 1) {
                world.setBlockState(pos1, AllBlocks.SHAFT.getDefaultState().with(AXIS, getMagneticAxis(state1)));
            }
        });

        playRemoveSound(world, pos);
        return ActionResult.SUCCESS;
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return ModdedItems.COPPER_COIL.asStack();
    }

    public static float resistance() {
        return 0.1f;
    }
}
