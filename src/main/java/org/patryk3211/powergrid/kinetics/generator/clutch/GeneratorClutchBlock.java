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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.kinetics.generator.IRotorAssemblyPart;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;

public class GeneratorClutchBlock extends DirectionalKineticBlock implements IBE<GeneratorClutchBlockEntity>, IRotorAssemblyPart {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final VoxelShaper SHAPER = VoxelShaper.forDirectional(Shapes.or(
            box(0, 0, 0, 16, 16, 10),
            box(4, 4, 10, 12, 12, 16)
    ), Direction.NORTH).withVerticalShapes(Shapes.or(
            box(0, 6, 0, 16, 16, 16),
            box(4, 0, 4, 12, 6, 12)
    ));

    public GeneratorClutchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var world = context.getLevel();
        InteractionResult result = super.onWrenched(state, context);
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
        builder.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var initial = super.getStateForPlacement(context);
        if(initial == null)
            return null;
        return initial
                .setValue(FACING, initial.getValue(FACING).getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPER.get(state.getValue(FACING));
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean isMoving) {
        if(world.isClientSide)
            return;
        var powered = world.hasNeighborSignal(pos);
        world.setBlock(pos, state.setValue(POWERED, powered), UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE);
        withBlockEntityDo(world, pos, be -> {
            be.updateStrength(world.getBestNeighborSignal(pos));
        });
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(FACING) == face;
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
        return state.getValue(FACING) == dir.getOpposite();
    }

    @Override
    public float getInertia() {
        return ModdedConfigs.server().kinetics.generatorControls.generatorClutchInertia.getF();
    }

    @Override
    public Direction getPreferredFacing(BlockPlaceContext context) {
        var preferredFacing = super.getPreferredFacing(context);
        if(preferredFacing != null)
            return preferredFacing;

        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        for(Direction facing : Direction.values()) {
            BlockState state = world.getBlockState(pos.relative(facing, -1));
            if(state.getBlock() instanceof IRotorAssemblyPart assembly && assembly.canConnect(state, facing)) {
                if(preferredFacing == null) {
                    preferredFacing = facing;
                    continue;
                }
                return null;
            }
        }
        return preferredFacing;
    }
}
