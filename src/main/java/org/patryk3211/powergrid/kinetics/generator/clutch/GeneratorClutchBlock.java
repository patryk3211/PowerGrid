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
package org.patryk3211.powergrid.kinetics.generator.clutch;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.kinetics.generator.IRotorAssemblyPart;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;

public class GeneratorClutchBlock extends DirectionalKineticBlock implements IBE<GeneratorClutchBlockEntity>, IRotorAssemblyPart {
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final VoxelShaper SHAPER = VoxelShaper.forDirectional(VoxelShapes.union(
            createCuboidShape(0, 0, 0, 16, 16, 10),
            createCuboidShape(4, 4, 10, 12, 12, 16)
    ), Direction.NORTH).withVerticalShapes(VoxelShapes.union(
            createCuboidShape(0, 6, 0, 16, 16, 16),
            createCuboidShape(4, 0, 4, 12, 6, 12)
    ));

    public GeneratorClutchBlock(Settings properties) {
        super(properties);
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        var world = context.getWorld();
        ActionResult result = super.onWrenched(state, context);
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
        builder.add(POWERED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        var initial = super.getPlacementState(context);
        if(initial == null)
            return null;
        return initial
                .with(FACING, initial.get(FACING).getOpposite())
                .with(POWERED, context.getWorld().isReceivingRedstonePower(context.getBlockPos()));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPER.get(state.get(FACING));
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean isMoving) {
        if(world.isClient)
            return;
        var powered = world.isReceivingRedstonePower(pos);
        world.setBlockState(pos, state.with(POWERED, powered), NOTIFY_LISTENERS | FORCE_STATE);
        withBlockEntityDo(world, pos, be -> {
            be.updateStrength(world.getReceivedRedstonePower(pos));
        });
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.get(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
        return state.get(FACING) == face;
    }

    @Override
    public Class<GeneratorClutchBlockEntity> getBlockEntityClass() {
        return GeneratorClutchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GeneratorClutchBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.GENERATOR_CLUTCH.get();
    }

    @Override
    public boolean canConnect(BlockState state, Direction dir) {
        // Facing side is the kinetic input, opposite is the rotor assembly.
        return state.get(FACING) == dir.getOpposite();
    }
}
