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
package org.patryk3211.powergrid.kinetics.base;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.advancements.PGAdvancementBehaviour;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public abstract class ElectricKineticBlock extends KineticBlock implements IElectric {
    private BlockStateTerminalCollection terminals = null;
    private ImmutableMap<BlockState, VoxelShape> outlines = null;

    public ElectricKineticBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        PGAdvancementBehaviour.setPlacedBy(level, pos, placer);
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
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var result = super.onWrenched(state, context);
        if(result == InteractionResult.SUCCESS && !context.getLevel().isClientSide)
            ElectricBlock.refreshConnectionEntities(context.getLevel(), context.getClickedPos());
        return result;
    }

    public float resistance() {
        return ResistanceValues.get(this);
    }

    public float resistance(String suffix) {
        return ResistanceValues.get(this, suffix);
    }
}
