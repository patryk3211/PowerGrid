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
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.loot.context.LootContextParameterSet;
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
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class WindingBlock extends ElectricBlock implements IBE<WindingBlockEntity> {
    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;
    public static final IntProperty PART = IntProperty.of("part", 0, 2);
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;

    private static final VoxelShaper HORIZONTAL_END_SHAPER = VoxelShaper.forDirectional(VoxelShapes.union(
            createCuboidShape(2, 3, 3, 14, 13, 16),
            createCuboidShape(0, 6, 6, 16, 10, 10)
    ), Direction.SOUTH);
    private static final VoxelShaper VERTICAL_END_SHAPER = VoxelShaper.forDirectional(VoxelShapes.union(
            createCuboidShape(3, 2, 3, 13, 14, 16),
            createCuboidShape(6, 0, 6, 10, 16, 10)
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
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 6, 6, 3, 10, 10, 4)
                    .withOrigin(8, 8, 3)
                    .withColor(IDecoratedTerminal.RED);
    private static final TerminalBoundingBox TERMINAL_NEGATIVE =
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 6, 6, 12, 10, 10, 13)
                    .withOrigin(8, 8, 13)
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
                }, ALONG_FIRST_AXIS)
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
        builder.add(AXIS, PART, ALONG_FIRST_AXIS);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);

        var dir = sourcePos.subtract(pos);
        if(Direction.fromVector(dir.getX(), dir.getY(), dir.getZ()).getAxis() == state.get(AXIS))
            return;

        withBlockEntityDo(world, pos, be -> be.onNeighborChanged(sourcePos));
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

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        var stacks = super.getDroppedStacks(state, builder);
//        if(state.get(PART) != 1)
//            stacks.add(AllBlocks.SHAFT.asStack());
        return stacks;
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
        return ActionResult.FAIL;
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

    public static float resistance() {
        return 0.1f;
    }
}
