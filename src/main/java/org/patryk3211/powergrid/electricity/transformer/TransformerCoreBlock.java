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
package org.patryk3211.powergrid.electricity.transformer;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.patryk3211.powergrid.collections.ModdedBlocks;

public class TransformerCoreBlock extends Block implements IWrenchable {
    public TransformerCoreBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        var pos = context.getBlockPos();
        var world = context.getWorld();
        world.setBlockState(pos, ModdedBlocks.TRANSFORMER_SMALL.getDefaultState()
                .with(TransformerSmallBlock.HORIZONTAL_AXIS, context.getHorizontalPlayerFacing().rotateYClockwise().getAxis()));

        playRotateSound(world, pos);
        return ActionResult.SUCCESS;
    }
}
