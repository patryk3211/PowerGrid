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
package org.patryk3211.powergrid.electricity.base;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ElectricBlock extends Block implements IElectric {
    private BlockStateTerminalCollection terminals = null;
    private ImmutableMap<BlockState, VoxelShape> outlines = null;

    public ElectricBlock(Properties settings) {
        super(settings);
    }

    protected void setTerminalCollection(BlockStateTerminalCollection terminals) {
        this.terminals = terminals;
        var mapper = terminals.shapeMapper();
        if(mapper != null)
            outlines = getShapeForEachState(mapper);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        IBE.onRemove(state, world, pos, newState);
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
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var result = IElectric.super.onWrenched(state, context);
        if(result == InteractionResult.SUCCESS && !context.getLevel().isClientSide)
            refreshConnectionEntities(context.getLevel(), context.getClickedPos());
        return result;
    }

    public static void refreshConnectionEntities(Level world, BlockPos pos) {
        var behaviour = BlockEntityBehaviour.get(world, pos, ElectricBehaviour.TYPE);
        if(behaviour != null) {
            behaviour.refreshConnectionEntities();
        }
    }

    public float resistance() {
        return ResistanceValues.get(this);
    }

    public float resistance(String suffix) {
        return ResistanceValues.get(this, suffix);
    }
}
