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
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

;

public class WindingBlock extends ElectricBlock implements IBE<WindingBlockEntity> {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;

    public static final BooleanProperty CASE_RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty CASE_LEFT = BooleanProperty.create("left");

    private static final VoxelShaper HORIZONTAL_END_SHAPER = VoxelShaper.forDirectional(Shapes.or(
            box(2, 3, 3, 14, 13, 16),
            box(0, 6, 6, 16, 10, 10),
            box(6, 6, 0, 10, 10, 3)
    ), Direction.SOUTH);
    private static final VoxelShaper VERTICAL_END_SHAPER = VoxelShaper.forDirectional(Shapes.or(
            box(3, 2, 3, 13, 14, 16),
            box(6, 0, 6, 10, 16, 10),
            box(6, 6, 0, 10, 10, 3)
    ), Direction.SOUTH);

    private static final VoxelShaper HORIZONTAL_MIDDLE_SHAPER = VoxelShaper.forAxis(
            box(2, 3, 0, 14, 13, 16),
            Direction.Axis.Z
    );
    private static final VoxelShaper VERTICAL_MIDDLE_SHAPER = VoxelShaper.forAxis(
            box(3, 2, 0, 13, 14, 16),
            Direction.Axis.Z
    );

    private static final TerminalBoundingBox TERMINAL_POSITIVE =
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 6, 6, 0, 10, 10, 2)
                    .withColor(IDecoratedTerminal.RED);
    private static final TerminalBoundingBox TERMINAL_NEGATIVE =
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 6, 6, 14, 10, 10, 16)
                    .withColor(IDecoratedTerminal.BLUE);

    public WindingBlock(Properties settings) {
        super(settings);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStatesExcept(state -> {
                    var terminals = new TerminalBoundingBox[2];
                    var part = state.getValue(PART);
                    if(part == 1)
                        return terminals;
                    var terminalIn = part == 0 ? TERMINAL_POSITIVE : TERMINAL_NEGATIVE;
                    switch(state.getValue(AXIS)) {
                        case X -> terminalIn = terminalIn.rotateAroundY(-90);
                        case Y -> terminalIn = terminalIn.rotateAroundX(90);
                    }
                    terminals[part / 2] = terminalIn;
                    return terminals;
                }, ALONG_FIRST_AXIS, CASE_RIGHT, CASE_LEFT)
                .build());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        var part = state.getValue(PART);
        if(part == 0 || part == 2) {
            var dir = Direction.fromAxisAndDirection(state.getValue(AXIS), part == 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            var along = state.getValue(ALONG_FIRST_AXIS);
            var shaper = along ? VERTICAL_END_SHAPER : HORIZONTAL_END_SHAPER;
            return shaper.get(dir);
        } else {
            var along = state.getValue(ALONG_FIRST_AXIS);
            var shaper = along ? VERTICAL_MIDDLE_SHAPER : HORIZONTAL_MIDDLE_SHAPER;
            return shaper.get(state.getValue(AXIS));
        }
    }

    private void walkForward(LevelAccessor world, BlockPos pos, Direction.Axis axis, BiConsumer<BlockPos, BlockState> callback) {
        BlockState state;
        boolean last = false;
        while(!last) {
            pos = pos.relative(axis, 1);
            state = world.getBlockState(pos);
            if(!state.is(this))
                return;
            if(state.getValue(PART) == 2)
                last = true;
            callback.accept(pos, state);
        }
    }

    public void walkBackward(LevelAccessor world, BlockPos pos, Direction.Axis axis, BiConsumer<BlockPos, BlockState> callback) {
        BlockState state;
        boolean last = false;
        while(!last) {
            pos = pos.relative(axis, -1);
            state = world.getBlockState(pos);
            if(!state.is(this))
                return;
            if(state.getValue(PART) == 0)
                last = true;
            callback.accept(pos, state);
        }
    }

    public void walk(LevelAccessor world, BlockPos pos, BiConsumer<BlockPos, BlockState> callback) {
        var state = world.getBlockState(pos);
        if(!state.is(this))
            return;
        callback.accept(pos, state);
        var axis = state.getValue(AXIS);
        switch(state.getValue(PART)) {
            case 0 -> walkForward(world, pos, axis, callback);
            case 1 -> {
                walkForward(world, pos, axis, callback);
                walkBackward(world, pos, axis, callback);
            }
            case 2 -> walkBackward(world, pos, axis, callback);
        }
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(world, player, pos, state, blockEntity, tool);
        if(state.getValue(PART) != 1) {
            popResource(world, pos, AllBlocks.SHAFT.asStack());
        }
    }

    @Override
    public void destroy(LevelAccessor world, BlockPos pos, BlockState state) {
        super.destroy(world, pos, state);
        if(world.isClientSide())
            return;
        var axis = state.getValue(AXIS);
        BiConsumer<BlockPos, BlockState> breakBlock = (pos1, state1) -> {
            world.destroyBlock(pos1, true);
            if(state1.getValue(PART) != 1) {
                world.setBlock(pos1, AllBlocks.SHAFT.getDefaultState().setValue(AXIS, getMagneticAxis(state1)), UPDATE_ALL);
            }
        };
        switch(state.getValue(PART)) {
            case 0 -> walkForward(world, pos, axis, breakBlock);
            case 1 -> {
                walkForward(world, pos, axis, breakBlock);
                walkBackward(world, pos, axis, breakBlock);
            }
            case 2 -> walkBackward(world, pos, axis, breakBlock);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS, PART, ALONG_FIRST_AXIS, CASE_RIGHT, CASE_LEFT);
    }

    public static boolean canConnect(BlockState thisState, boolean positive, BlockState state) {
        if(state.getBlock() instanceof WindingBlock windingBlock) {
            // Another winding, check for alignment
            if(state.getValue(AXIS) == thisState.getValue(AXIS) && state.getValue(ALONG_FIRST_AXIS) == thisState.getValue(ALONG_FIRST_AXIS)) {
                // Alignment matches and block entity is valid, these can be connected.
                return true;
            }
        } else if(state.getBlock() instanceof GeneratorHousing) {
            var windingBlock = (WindingBlock) thisState.getBlock();
            var parallelAxis = windingBlock.getParallelCheckAxis(thisState);
            if(parallelAxis.isHorizontal()) {
                var expectedFacing = Direction.fromAxisAndDirection(parallelAxis, positive ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
                return state.getValue(HORIZONTAL_FACING) == expectedFacing;
            } else {
                var expectUp = !positive;
                return state.getValue(UP) == expectUp;
            }
        }
        return false;
    }

    public void updateCase(BlockState state, LevelAccessor world, BlockPos pos) {
        var axis = getParallelCheckAxis(state);
        var stateN = world.getBlockState(pos.relative(axis, -1));
        var left = canConnect(state, false, stateN);

        var stateP = world.getBlockState(pos.relative(axis, 1));
        var right = canConnect(state, true, stateP);

        var newState = state.setValue(CASE_LEFT, left)
                .setValue(CASE_RIGHT, right);
        if(newState != state) {
            world.setBlock(pos, newState, 0);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
        updateCase(state, world, pos);

        var dir = sourcePos.subtract(pos);
        if(Direction.fromDelta(dir.getX(), dir.getY(), dir.getZ()).getAxis() == state.getValue(AXIS))
            return;

        withBlockEntityDo(world, pos, be -> be.onNeighborChanged(sourcePos));
    }

    @Override
    public void updateIndirectNeighbourShapes(BlockState state, LevelAccessor world, BlockPos pos, int flags, int maxUpdateDepth) {
        super.updateIndirectNeighbourShapes(state, world, pos, flags, maxUpdateDepth);
        updateCase(state, world, pos);
    }

    @Nullable
    public BlockPos getMainBlockPos(Level world, BlockPos pos) {
        var state = world.getBlockState(pos);
        if(!state.is(this))
            return null;
        var axis = state.getValue(AXIS);
        switch(state.getValue(PART)) {
            case 0 -> {
                return pos;
            }
            case 1, 2 -> {
                while(true) {
                    pos = pos.relative(axis, -1);
                    state = world.getBlockState(pos);
                    if(!state.is(this))
                        return null;
                    if(state.getValue(PART) == 0)
                        return pos;
                }
            }
            default -> {
                return null;
            }
        }
    }

    public Optional<WindingBlockEntity> getMainBlockEntity(Level world, BlockPos pos) {
        var mainPos = getMainBlockPos(world, pos);
        if(mainPos != null)
            return world.getBlockEntity(mainPos, ModdedBlockEntities.WINDING.get());
        return Optional.empty();
    }

    @Override
    public ElectricBehaviour getBehaviour(Level world, BlockPos pos, BlockState state) {
        return getMainBlockEntity(world, pos)
                .map(winding -> winding.getBehaviourProvider().getBehaviour(ElectricBehaviour.TYPE))
                .orElse(null);
    }

    public Direction.Axis getParallelCheckAxis(BlockState state) {
        var along = state.getValue(ALONG_FIRST_AXIS);
        return switch(state.getValue(AXIS)) {
            case X -> along ? Direction.Axis.Z : Direction.Axis.Y;
            case Y -> along ? Direction.Axis.X : Direction.Axis.Z;
            case Z -> along ? Direction.Axis.X : Direction.Axis.Y;
        };
    }

    public Direction.Axis getMagneticAxis(BlockState state) {
        var along = state.getValue(ALONG_FIRST_AXIS);
        return switch(state.getValue(AXIS)) {
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
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();

        var part = state.getValue(PART);
        var axis = state.getValue(AXIS);
        switch(part) {
            case 0 -> {
                if(world.getBlockState(pos.relative(axis, 1)).getValue(PART) != 1)
                    return InteractionResult.FAIL;
            }
            case 1 -> {
                return InteractionResult.FAIL;
            }
            case 2 -> {
                if(world.getBlockState(pos.relative(axis, -1)).getValue(PART) != 1)
                    return InteractionResult.FAIL;
            }
        }

        boolean shouldBreak = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, world.getBlockState(pos), null);
        if(!shouldBreak)
            return InteractionResult.SUCCESS;

        if(!(world instanceof ServerLevel serverWorld))
            return InteractionResult.SUCCESS;

        if(player != null && !player.isCreative()) {
            Block.getDrops(state.setValue(PART, 1), serverWorld, pos, world.getBlockEntity(pos), player, context.getItemInHand())
                    .forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
        }
        state.setValue(PART, 1).spawnAfterBreak(serverWorld, pos, ItemStack.EMPTY, true);
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var newPos = pos.relative(axis, 1 - part);
        world.setBlockAndUpdate(newPos, state);
        if(state.getValue(PART) == 0)
            withBlockEntityDo(world, newPos, WindingBlockEntity::makeMain);

        IWrenchable.playRemoveSound(world, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();

        if(!(world instanceof ServerLevel serverLevel))
            return InteractionResult.SUCCESS;

        boolean shouldBreak = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, world.getBlockState(pos), null);
        if(!shouldBreak)
            return InteractionResult.SUCCESS;

        walk(world, pos, (pos1, state1) -> {
            if (player != null && !player.isCreative()) {
                Block.getDrops(state1, serverLevel, pos1, world.getBlockEntity(pos1), player, context.getItemInHand())
                        .forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
                if(pos.equals(pos1) && state1.getValue(PART) != 1) {
                    player.getInventory().placeItemBackInInventory(AllBlocks.SHAFT.asStack());
                }
            }
            state1.spawnAfterBreak(serverLevel, pos1, ItemStack.EMPTY, true);
            world.destroyBlock(pos1, false);
            if(!pos.equals(pos1) && state1.getValue(PART) != 1) {
                world.setBlockAndUpdate(pos1, AllBlocks.SHAFT.getDefaultState().setValue(AXIS, getMagneticAxis(state1)));
            }
        });

        IWrenchable.playRemoveSound(world, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        return ModdedItems.COPPER_COIL.asStack();
    }
}
