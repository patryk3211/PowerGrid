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
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public abstract class ElectricKineticBlock extends KineticBlock implements IElectric {
    private BlockStateTerminalCollection terminals = null;
    private ImmutableMap<BlockState, VoxelShape> outlines = null;

    public ElectricKineticBlock(Settings properties) {
        super(properties);
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
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        var result = super.onWrenched(state, context);
        if(result == ActionResult.SUCCESS && !context.getWorld().isClient)
            ElectricBlock.refreshConnectionEntities(context.getWorld(), context.getBlockPos());
        return result;
    }
}
