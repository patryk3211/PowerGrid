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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedBlocks;

import static org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlock.PART;

public class TransformerCoreBlock extends Block implements IWrenchable {
    public TransformerCoreBlock(Settings settings) {
        super(settings);
    }

    private boolean locate2x2(World world, BlockPos pos, Direction dir) {
        boolean[] isCore = new boolean[3 * 3];

        for(int x = -1; x <= 1; ++x) {
            for(int y = -1; y <= 1; ++y) {
                var i = x + 1;
                var j = y + 1;
                var oPos = pos.offset(dir, x).offset(Direction.UP, y);
                isCore[i + j * 3] = world.getBlockState(oPos).isOf(this);
            }
        }

        for(int x = -1; x < 1; ++x) {
            for(int y = -1; y < 1; ++y) {
                var i = x + 1;
                var j = y + 1;
                if(isCore[i + j * 3] && isCore[i + 1 + j * 3] && isCore[i + (j + 1) * 3] && isCore[i + 1 + (j + 1) * 3]) {
                    // 2x2 section of transformer core found.
                    if(!world.isClient) {
                        var state = ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                                .with(TransformerMediumBlock.HORIZONTAL_AXIS, dir.getAxis());
                        world.setBlockState(pos.offset(dir, x).offset(Direction.UP, y), state.with(PART, 0));
                        world.setBlockState(pos.offset(dir, x + 1).offset(Direction.UP, y), state.with(PART, 1));
                        world.setBlockState(pos.offset(dir, x).offset(Direction.UP, y + 1), state.with(PART, 2));
                        world.setBlockState(pos.offset(dir, x + 1).offset(Direction.UP, y + 1), state.with(PART, 3));
                    }
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        var pos = context.getBlockPos();
        var world = context.getWorld();
        if (!locate2x2(world, pos, Direction.SOUTH) && !locate2x2(world, pos, Direction.EAST)) {
            // 1x1 transformer
            if(!world.isClient) {
                world.setBlockState(pos, ModdedBlocks.TRANSFORMER_SMALL.getDefaultState()
                        .with(TransformerSmallBlock.HORIZONTAL_AXIS, context.getHorizontalPlayerFacing().rotateYClockwise().getAxis()));
            }
        }
        playRotateSound(world, pos);
        return ActionResult.SUCCESS;
    }
}
