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

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.wire.powercord.IAcceptCord;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlock;

import java.util.List;

import static org.patryk3211.powergrid.PowerGrid.maxRPM;
import static org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlock.NORTH_SHAPE;
import static org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlock.UP_SHAPE;
import static org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlockEntity.CONVERSION_CONSTANT;

public class ConstantSpeedMotorBlock extends ElectricKineticBlock implements IBE<ConstantSpeedMotorBlockEntity>, IHaveElectricProperties, IAcceptCord {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final TerminalBoundingBox[] NORTH_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 5, 13, 14, 7, 14, 16)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 9, 13, 14, 11, 14, 16)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    public ConstantSpeedMotorBlock(Properties properties) {
        super(properties);
        setTerminalCollection(DirectionalElectricBlock.directionalNorthTerminals(this, NORTH_TERMINALS, NORTH_SHAPE, UP_SHAPE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    public Direction getPreferredFacing(BlockPlaceContext context) {
        Direction prefferedSide = null;
        for (Direction side : Iterate.directions) {
            BlockState blockState = context.getLevel()
                    .getBlockState(context.getClickedPos()
                            .relative(side));
            if (blockState.getBlock() instanceof IRotate) {
                if (((IRotate) blockState.getBlock()).hasShaftTowards(context.getLevel(), context.getClickedPos()
                        .relative(side), blockState, side.getOpposite()))
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredFacing(context);
        if (preferred == null || (context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown())) {
            Direction nearestLookingDirection = context.getNearestLookingDirection();
            return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer()
                    .isShiftKeyDown() ? nearestLookingDirection : nearestLookingDirection.getOpposite());
        }
        return defaultBlockState().setValue(FACING, preferred.getOpposite());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Class<ConstantSpeedMotorBlockEntity> getBlockEntityClass() {
        return ConstantSpeedMotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ConstantSpeedMotorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CONSTANT_SPEED_MOTOR.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(resistance(), player, tooltip);
        var torque = BlockStressValues.getCapacity(this) * ModdedConfigs.server().kinetics.torqueForStress.getF();
        var maxPower = maxRPM() * torque / CONVERSION_CONSTANT;
        Voltage.max((int) Math.sqrt(maxPower * resistance()), player, tooltip);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    public boolean renderPlug() {
        return true;
    }
}
