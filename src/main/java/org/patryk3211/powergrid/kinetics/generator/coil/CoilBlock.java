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
package org.patryk3211.powergrid.kinetics.generator.coil;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.IConnectableBlock;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;

public class CoilBlock extends ElectricBlock implements IBE<CoilBlockEntity>, IConnectableBlock, IHaveElectricProperties {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty HAS_TERMINALS = BooleanProperty.of("terminals");

    private static final TerminalBoundingBox[] UP_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 3, 0, 3, 6, 2, 6)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 10, 0, 10, 13, 2, 13)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE_UP = createCuboidShape(0, 2, 0,16, 14, 16);

    public CoilBlock(Settings settings) {
        super(settings);

        var shaper = VoxelShaper.forDirectional(SHAPE_UP, Direction.UP);
        setTerminalCollection(BlockStateTerminalCollection
                .builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(UP_TERMINALS, terminal -> {
                    if(!state.get(HAS_TERMINALS))
                        return null;
                    return switch(state.get(FACING)) {
                        case UP -> terminal;
                        case DOWN -> terminal.rotateAroundX(180);
                        case NORTH -> terminal.rotateAroundX(90);
                        case SOUTH -> terminal.rotateAroundX(-90);
                        case EAST -> terminal.rotateAroundX(90).rotateAroundY(90);
                        case WEST -> terminal.rotateAroundX(90).rotateAroundY(-90);
                    };
                }))
                .withShapeMapper(state -> shaper.get(state.get(FACING)))
                .build()
        );
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        if(context.getSide() == state.get(FACING).getOpposite()) {
            var world = context.getWorld();
            if(!world.isClient) {
                boolean hasTerminals = !state.get(HAS_TERMINALS);
                if (world.getBlockEntity(context.getBlockPos()) instanceof CoilBlockEntity coil) {
                    if (hasTerminals) {
                        coil.getAggregate().makeOutput(coil);
                    } else {
                        coil.getAggregate().removeOutput(coil);
                    }
                }
                return ActionResult.SUCCESS;
            }
        }
        var result = super.onWrenched(state, context);
        if(result == ActionResult.SUCCESS && !context.getWorld().isClient) {
            var behaviour = BlockEntityBehaviour.get(context.getWorld(), context.getBlockPos(), CoilBehaviour.TYPE);
            if(behaviour != null) {
                behaviour.grabRotor();
            }
        }
        return result;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        var behaviour = BlockEntityBehaviour.get(world, pos, CoilBehaviour.TYPE);
        if(behaviour != null)
            behaviour.onNeighborChanged(sourcePos);
        if(world.getBlockEntity(pos) instanceof CoilBlockEntity coil) {
            coil.rebuildAggregate();
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, HAS_TERMINALS);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var preferredFacing = getPreferredFacing(ctx.getBlockPos(), ctx.getWorld());
        var facing = preferredFacing == null || ctx.getPlayer() == null || ctx.getPlayer().isSneaking() ? ctx.getPlayerLookDirection() : preferredFacing;
        return getDefaultState()
                .with(FACING, facing)
                .with(HAS_TERMINALS, false);
    }

    @Override
    public boolean connects(BlockState state, Direction side, BlockState checkState) {
        // Coils only connect if their facing is the same
        if(checkState.isOf(this) && state.get(FACING) == checkState.get(FACING))
            return true;
        var housing = ModdedBlocks.GENERATOR_HOUSING.get();
        if(checkState.isOf(housing)) {
            // Use the housing implementation for this check.
            return housing.connects(checkState, side.getOpposite(), state);
        }
        return false;
    }

    @Override
    public boolean canPropagate(BlockState state, Direction direction) {
        return state.get(FACING).getAxis() != direction.getAxis();
    }

    @Nullable
    public Direction getPreferredFacing(BlockPos pos, World world) {
        Direction facing = null;
        for(var dir : Direction.values()) {
            var state = world.getBlockState(pos.offset(dir));
            if(state.isOf(this)) {
                if(facing != null)
                    return null;
                facing = state.get(FACING);
                if(facing.getAxis() == dir.getAxis())
                    facing = null;
            }
        }
        return facing;
    }

    @Override
    public Class<CoilBlockEntity> getBlockEntityClass() {
        return CoilBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CoilBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.GENERATOR_COIL.get();
    }

    public static float resistance() {
        return 0.1f;
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        Resistance.series(resistance(), player, tooltip);
    }
}
