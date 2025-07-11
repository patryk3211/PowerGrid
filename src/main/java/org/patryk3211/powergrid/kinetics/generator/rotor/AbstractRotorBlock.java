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
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.kinetics.generator.IRotorAssemblyPart;

public abstract class AbstractRotorBlock extends Block implements IRotorAssemblyPart, IWrenchable {
    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;

    public AbstractRotorBlock(Settings properties) {
        super(properties);
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        var world = context.getWorld();
        ActionResult result = IWrenchable.super.onWrenched(state, context);
        if(!result.isAccepted())
            return result;

        var behaviour = BlockEntityBehaviour.get(world, context.getBlockPos(), RotorBehaviour.TYPE);
        if(behaviour != null)
            behaviour.checkConnectivity(null);

        return result;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(AXIS);
    }

    public boolean hasPositive(WorldView world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(switch(axis) {
            case X -> pos.east();
            case Y -> pos.up();
            case Z -> pos.south();
        });
        return state.getBlock() instanceof IRotorAssemblyPart assembly && assembly.canConnect(state, Direction.from(axis, Direction.AxisDirection.NEGATIVE));
    }

    public boolean hasNegative(WorldView world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(switch(axis) {
            case X -> pos.west();
            case Y -> pos.down();
            case Z -> pos.north();
        });
        return state.getBlock() instanceof IRotorAssemblyPart assembly && assembly.canConnect(state, Direction.from(axis, Direction.AxisDirection.POSITIVE));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();

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
            preferredAxis = context.getPlayerLookDirection().getAxis();

        return getDefaultState()
                .with(AXIS, preferredAxis);
    }

    @Override
    public boolean canConnect(BlockState state, Direction dir) {
        return state.get(AXIS) == dir.getAxis();
    }
}
