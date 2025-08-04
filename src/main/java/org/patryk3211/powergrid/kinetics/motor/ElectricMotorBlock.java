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
package org.patryk3211.powergrid.kinetics.motor;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlock;

import java.util.List;

public class ElectricMotorBlock extends ElectricKineticBlock implements IBE<ElectricMotorBlockEntity>, IHaveElectricProperties {
    public static final DirectionProperty FACING = Properties.FACING;

    private static final VoxelShape NORTH_SHAPE = createCuboidShape(3, 3, 0, 13, 13, 16);

    private static final TerminalBoundingBox[] NORTH_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 2.5, 11.5, 6.5, 4.5, 13.5, 9.5)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 11.5, 11.5, 6.5, 13.5, 13.5, 9.5)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    public ElectricMotorBlock(Settings properties) {
        super(properties);
        setTerminalCollection(DirectionalElectricBlock.directionalNorthTerminals(this, NORTH_TERMINALS, NORTH_SHAPE));
//        setTerminalCollection(BlockStateTerminalCollection.builder(this)
//                .forAllStates(state -> BlockStateTerminalCollection.each(NORTH_TERMINALS, terminal -> switch(state.get(FACING)) {
//                    case NORTH -> terminal;
//                    case SOUTH -> terminal.rotateAroundY(180);
//                    case EAST -> terminal.rotateAroundY(90);
//                    case WEST -> terminal.rotateAroundY(-90);
//                    case UP -> terminal.rotateAroundX(-90);
//                    case DOWN -> terminal.rotateAroundX(90);
//                }))
//                .withShapeMapper(state -> shaper.get(state.get(FACING)))
//                .build());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    public Direction getPreferredFacing(ItemPlacementContext context) {
        Direction prefferedSide = null;
        for (Direction side : Iterate.directions) {
            BlockState blockState = context.getWorld()
                    .getBlockState(context.getBlockPos()
                            .offset(side));
            if (blockState.getBlock() instanceof IRotate) {
                if (((IRotate) blockState.getBlock()).hasShaftTowards(context.getWorld(), context.getBlockPos()
                        .offset(side), blockState, side.getOpposite()))
                    if (prefferedSide != null && prefferedSide.getAxis() != side.getAxis()) {
                        prefferedSide = null;
                        break;
                    } else {
                        prefferedSide = side;
                    }
            }
        }
        return prefferedSide == null ? null : prefferedSide.getOpposite();
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        Direction preferred = getPreferredFacing(context);
        if (preferred == null || (context.getPlayer() != null && context.getPlayer()
                .isSneaking())) {
            Direction nearestLookingDirection = context.getPlayerLookDirection();
            return getDefaultState().with(FACING, context.getPlayer() != null && context.getPlayer()
                    .isSneaking() ? nearestLookingDirection : nearestLookingDirection.getOpposite());
        }
        return getDefaultState().with(FACING, preferred.getOpposite());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.get(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
        return face == state.get(FACING);
    }

    @Override
    public Class<ElectricMotorBlockEntity> getBlockEntityClass() {
        return ElectricMotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ElectricMotorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.ELECTRIC_MOTOR.get();
    }

    public static float resistance() {
        return 10f;
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        Resistance.series(resistance(), player, tooltip);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, BlockMirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.get(FACING)));
    }
}
