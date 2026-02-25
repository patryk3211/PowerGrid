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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedBlocks;

import static org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlock.PART;

public class TransformerCoreBlock extends Block implements IWrenchable {
    public TransformerCoreBlock(Properties settings) {
        super(settings);
    }

    private boolean locate2x2(Level world, BlockPos pos, Direction dir) {
        boolean[] isCore = new boolean[3 * 3];

        for(int x = -1; x <= 1; ++x) {
            for(int y = -1; y <= 1; ++y) {
                var i = x + 1;
                var j = y + 1;
                var oPos = pos.relative(dir, x).relative(Direction.UP, y);
                isCore[i + j * 3] = world.getBlockState(oPos).is(this);
            }
        }

        for(int x = -1; x < 1; ++x) {
            for(int y = -1; y < 1; ++y) {
                var i = x + 1;
                var j = y + 1;
                if(isCore[i + j * 3] && isCore[i + 1 + j * 3] && isCore[i + (j + 1) * 3] && isCore[i + 1 + (j + 1) * 3]) {
                    // 2x2 section of transformer core found.
                    if(!world.isClientSide) {
                        var state = ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                                .setValue(TransformerMediumBlock.HORIZONTAL_AXIS, dir.getAxis());
                        world.setBlock(pos.relative(dir, x).relative(Direction.UP, y), state.setValue(PART, 0), Block.UPDATE_CLIENTS);
                        world.setBlock(pos.relative(dir, x + 1).relative(Direction.UP, y), state.setValue(PART, 1), Block.UPDATE_CLIENTS);
                        world.setBlock(pos.relative(dir, x).relative(Direction.UP, y + 1), state.setValue(PART, 2), Block.UPDATE_CLIENTS);
                        world.setBlock(pos.relative(dir, x + 1).relative(Direction.UP, y + 1), state.setValue(PART, 3), Block.UPDATE_ALL);
                    }
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var pos = context.getClickedPos();
        var world = context.getLevel();
        if (!locate2x2(world, pos, Direction.SOUTH) && !locate2x2(world, pos, Direction.EAST)) {
            // 1x1 transformer
            if(!world.isClientSide) {
                world.setBlockAndUpdate(pos, ModdedBlocks.TRANSFORMER_SMALL.getDefaultState()
                        .setValue(TransformerSmallBlock.HORIZONTAL_AXIS, context.getHorizontalDirection().getClockWise().getAxis()));
            }
        }
        IWrenchable.playRotateSound(world, pos);
        return InteractionResult.SUCCESS;
    }
}
