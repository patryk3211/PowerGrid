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

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class ElectricBlock extends Block implements IElectric {
    public ElectricBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        IBE.onRemove(state, world, pos, newState);
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        var result = IElectric.super.onWrenched(state, context);
        if(result == ActionResult.SUCCESS && !context.getWorld().isClient)
            refreshConnectionEntities(context.getWorld(), context.getBlockPos());
        return result;
    }

    public static void refreshConnectionEntities(World world, BlockPos pos) {
        var behaviour = BlockEntityBehaviour.get(world, pos, ElectricBehaviour.TYPE);
        if(behaviour != null) {
            behaviour.refreshConnectionEntities();
        }
    }
}
