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
package org.patryk3211.powergrid.kinetics.generator.rotor;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.patryk3211.powergrid.kinetics.generator.IRotorAssemblyPart;

public abstract class AbstractRotorBlock extends Block implements IRotorAssemblyPart, IWrenchable {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public AbstractRotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var world = context.getLevel();
        InteractionResult result = IWrenchable.super.onWrenched(state, context);
        if(!result.consumesAction())
            return result;

        var behaviour = BlockEntityBehaviour.get(world, context.getClickedPos(), RotorBehaviour.TYPE);
        if(behaviour != null)
            behaviour.checkConnectivity(null);

        return result;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
    }

    public boolean hasPositive(LevelReader world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(switch(axis) {
            case X -> pos.east();
            case Y -> pos.above();
            case Z -> pos.south();
        });
        return state.getBlock() instanceof IRotorAssemblyPart assembly && assembly.canConnect(state, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
    }

    public boolean hasNegative(LevelReader world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(switch(axis) {
            case X -> pos.west();
            case Y -> pos.below();
            case Z -> pos.north();
        });
        return state.getBlock() instanceof IRotorAssemblyPart assembly && assembly.canConnect(state, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Direction.Axis preferredAxis = null;
        for(Direction.Axis axis : Direction.Axis.VALUES) {
            if(hasPositive(world, pos, axis) ||
                    hasNegative(world, pos, axis)) {
                if(preferredAxis != null) {
                    preferredAxis = null;
                    break;
                }
                preferredAxis = axis;
            }
        }

        if(preferredAxis == null)
            preferredAxis = context.getNearestLookingDirection().getAxis();

        return defaultBlockState()
                .setValue(AXIS, preferredAxis);
    }

    @Override
    public boolean canConnect(BlockState state, Direction dir) {
        return state.getValue(AXIS) == dir.getAxis();
    }
}
