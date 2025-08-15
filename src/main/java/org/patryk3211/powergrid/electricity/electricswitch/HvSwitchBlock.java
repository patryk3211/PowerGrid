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
package org.patryk3211.powergrid.electricity.electricswitch;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.*;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;

;

public class HvSwitchBlock extends HorizontalKineticBlock implements IElectric, IBE<HvSwitchBlockEntity>, IHaveElectricProperties {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 1);

    private static final VoxelShape SHAPE_0 = Shapes.or(
            box(0, 0, 0, 4, 16, 16),
            box(4, 0, 0, 12, 12, 12),
            box(12, 0, 0, 16, 16, 16)
    );

    private static final VoxelShape SHAPE_1 = Shapes.or(
            box(4, 0, 9, 12, 5, 15),
            box(4, 5, 10, 12, 12, 14)
    );

    private static final TerminalBoundingBox TERMINAL_0 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 12, 0, 10, 16, 4);
    private static final TerminalBoundingBox TERMINAL_1 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 0, 15, 10, 4, 16);

    private BlockStateTerminalCollection terminals = null;
    private ImmutableMap<BlockState, VoxelShape> outlines = null;

    public HvSwitchBlock(Properties properties) {
        super(properties);
        var shapers = new VoxelShaper[] {
                VoxelShaper.forHorizontal(SHAPE_0, Direction.SOUTH),
                VoxelShaper.forHorizontal(SHAPE_1, Direction.SOUTH)
        };
        setTerminalCollection(BlockStateTerminalCollection
                .builder(this)
                .forAllStatesExcept(state -> {
                    var part = state.getValue(PART);
                    var terminal = part == 0 ? TERMINAL_0 : TERMINAL_1;

                    var facing = state.getValue(HORIZONTAL_FACING);
                    terminal = terminal.rotateAroundY((int) facing.toYRot());

                    return part == 0 ?
                            new TerminalBoundingBox[] { terminal, null } :
                            new TerminalBoundingBox[] { null, terminal };
                })
                .withShapeMapper(state -> {
                    var part = state.getValue(PART);
                    var facing = state.getValue(HORIZONTAL_FACING);
                    return shapers[part].get(facing);
                })
                .build());
    }

    protected void setTerminalCollection(BlockStateTerminalCollection terminals) {
        this.terminals = terminals;
        var mapper = terminals.shapeMapper();
        if(mapper != null)
            outlines = getShapeForEachState(mapper);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if(outlines != null)
            return outlines.get(state);
        return super.getShape(state, world, pos, context);
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        if(terminals != null)
            return terminals.get(state, index);
        return null;
    }

    @Override
    public int terminalCount() {
        if(terminals != null)
            return terminals.count();
        return 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        var facing = state.getValue(HORIZONTAL_FACING);
        var neighbor = world.getBlockState(pos.relative(facing));
        return neighbor.canBeReplaced();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var preferred = getPreferredHorizontalFacing(context);
        if(context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            preferred = null;
        var facing = context.getHorizontalDirection();
        return defaultBlockState()
                .setValue(PART, 0)
                .setValue(HORIZONTAL_FACING, preferred != null ? preferred.getClockWise() : facing);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if(world.isClientSide || state.getValue(PART) != 0)
            return;
        var facing = state.getValue(HORIZONTAL_FACING);
        world.setBlockAndUpdate(pos.relative(facing), state.setValue(PART, 1));
    }

    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(world, pos, state, player);
        var facing = state.getValue(HORIZONTAL_FACING);
        var drop = player == null || !player.isCreative();
        if (state.getValue(PART) == 0) {
            world.destroyBlock(pos.relative(facing), drop);
        } else {
            world.destroyBlock(pos.relative(facing.getOpposite()), drop);
        }
    }

    @Override
    public ElectricBehaviour getBehaviour(Level world, BlockPos pos, BlockState state) {
        if(state.getValue(PART) == 0) {
            return IElectric.super.getBehaviour(world, pos, state);
        } else {
            return IElectric.super.getBehaviour(world, pos.relative(state.getValue(HORIZONTAL_FACING).getOpposite()), state);
        }
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        if(state.getValue(PART) == 1)
            return false;
        return getRotationAxis(state) == face.getAxis();
    }

    @Override
    public Class<HvSwitchBlockEntity> getBlockEntityClass() {
        return HvSwitchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HvSwitchBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.HV_SWITCH.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if(state.getValue(PART) == 0)
            return IBE.super.newBlockEntity(pos, state);
        return null;
    }

    @Override
    public boolean accepts(ItemStack wireStack) {
        return true;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        var result = super.onSneakWrenched(state, context);
        if(result == InteractionResult.SUCCESS && context.getLevel() instanceof ServerLevel serverWorld) {
            var pos = context.getClickedPos();
            var facing = state.getValue(HORIZONTAL_FACING);
            var world = context.getLevel();
            if(state.getValue(PART) == 0) {
                world.destroyBlock(pos.relative(facing), false);
            } else {
                var pos2 = pos.relative(facing.getOpposite());
                var player = context.getPlayer();
                if(player != null && !player.isCreative()) {
                    Block.getDrops(state, serverWorld, pos2, world.getBlockEntity(pos2), player, context.getItemInHand())
                            .forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
                }
                state.spawnAfterBreak(serverWorld, pos2, ItemStack.EMPTY, true);
                world.destroyBlock(pos2, false);
            }
        }
        return result;
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(ResistanceValues.get(this), player, tooltip);
    }
}
