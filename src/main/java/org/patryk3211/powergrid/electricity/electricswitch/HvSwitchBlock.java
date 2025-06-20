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
import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.*;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;

public class HvSwitchBlock extends HorizontalKineticBlock implements IElectric, IBE<HvSwitchBlockEntity>, IHaveElectricProperties {
    public static final IntProperty PART = IntProperty.of("part", 0, 1);

    private static final VoxelShape SHAPE_0 = VoxelShapes.union(
            createCuboidShape(0, 0, 0, 4, 16, 16),
            createCuboidShape(4, 0, 0, 12, 12, 12),
            createCuboidShape(12, 0, 0, 16, 16, 16)
    );

    private static final VoxelShape SHAPE_1 = VoxelShapes.union(
            createCuboidShape(4, 0, 9, 12, 5, 15),
            createCuboidShape(4, 5, 10, 12, 12, 14)
    );

    private static final TerminalBoundingBox TERMINAL_0 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 12, 0, 10, 16, 4);
    private static final TerminalBoundingBox TERMINAL_1 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 0, 15, 10, 4, 16);

    private BlockStateTerminalCollection terminals = null;
    private ImmutableMap<BlockState, VoxelShape> outlines = null;

    public HvSwitchBlock(Settings properties) {
        super(properties);
        var shapers = new VoxelShaper[] {
                VoxelShaper.forHorizontal(SHAPE_0, Direction.SOUTH),
                VoxelShaper.forHorizontal(SHAPE_1, Direction.SOUTH)
        };
        setTerminalCollection(BlockStateTerminalCollection
                .builder(this)
                .forAllStatesExcept(state -> {
                    var part = state.get(PART);
                    var terminal = part == 0 ? TERMINAL_0 : TERMINAL_1;

                    var facing = state.get(HORIZONTAL_FACING);
                    terminal = terminal.rotateAroundY((int) facing.asRotation());

                    return part == 0 ?
                            new TerminalBoundingBox[] { terminal, null } :
                            new TerminalBoundingBox[] { null, terminal };
                })
                .withShapeMapper(state -> {
                    var part = state.get(PART);
                    var facing = state.get(HORIZONTAL_FACING);
                    return shapers[part].get(facing);
                })
                .build());
    }

    protected void setTerminalCollection(BlockStateTerminalCollection terminals) {
        this.terminals = terminals;
        var mapper = terminals.shapeMapper();
        if(mapper != null)
            outlines = getShapesForStates(mapper);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(outlines != null)
            return outlines.get(state);
        return super.getOutlineShape(state, world, pos, context);
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
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(PART);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        var facing = state.get(HORIZONTAL_FACING);
        var neighbor = world.getBlockState(pos.offset(facing));
        return neighbor.isReplaceable();
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        var preferred = getPreferredHorizontalFacing(context);
        if(context.getPlayer() != null && context.getPlayer().isSneaking())
            preferred = null;
        var facing = context.getHorizontalPlayerFacing();
        return getDefaultState()
                .with(PART, 0)
                .with(HORIZONTAL_FACING, preferred != null ? preferred.rotateYClockwise() : facing);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if(world.isClient || state.get(PART) != 0)
            return;
        var facing = state.get(HORIZONTAL_FACING);
        world.setBlockState(pos.offset(facing), state.with(PART, 1));
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
        var facing = state.get(HORIZONTAL_FACING);
        if(state.get(PART) == 0) {
            world.breakBlock(pos.offset(facing), false);
        } else {
            world.breakBlock(pos.offset(facing.getOpposite()), false);
        }
    }

    @Override
    public ElectricBehaviour getBehaviour(World world, BlockPos pos, BlockState state) {
        if(state.get(PART) == 0) {
            return IElectric.super.getBehaviour(world, pos, state);
        } else {
            return IElectric.super.getBehaviour(world, pos.offset(state.get(HORIZONTAL_FACING).getOpposite()), state);
        }
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.get(HORIZONTAL_FACING).rotateYClockwise().getAxis();
    }

    @Override
    public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
        if(state.get(PART) == 1)
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
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        if(state.get(PART) == 0)
            return IBE.super.createBlockEntity(pos, state);
        return null;
    }

    @Override
    public boolean accepts(ItemStack wireStack) {
        return true;
    }

    public static float resistance() {
        return 0.1f;
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        return ActionResult.FAIL;
    }

    @Override
    public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
        var result = super.onSneakWrenched(state, context);
        if(result == ActionResult.SUCCESS) {
            var pos = context.getBlockPos();
            var facing = state.get(HORIZONTAL_FACING);
            var world = context.getWorld();
            if(state.get(PART) == 0) {
                world.breakBlock(pos.offset(facing), false);
            } else {
                world.breakBlock(pos.offset(facing.getOpposite()), false);
            }
        }
        return result;
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        Resistance.series(resistance(), player, tooltip);
    }
}
