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
package org.patryk3211.powergrid.electricity.carbonpile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedTags;

public class CarbonPileBlock extends Block {
    public static final BooleanProperty TOP = BooleanProperty.create("top");

    private static final VoxelShape SHAPE_MIDDLE = box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape SHAPE_TOP = Shapes.or(
            box(2, 0, 2, 14, 8, 14),
            box(4, 8, 4, 12, 12, 12)
    );

    public CarbonPileBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TOP);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TOP) ? SHAPE_TOP : SHAPE_MIDDLE;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        // We only care about changes in the Y axis
        if(pos.getX() != neighborPos.getX() || pos.getZ() != neighborPos.getZ())
            return;
        var current = pos;
        int size = 0;
        while(level.getBlockState(current).is(this)) {
            current = current.below();
            ++size;
        }
        var hasBase = ModdedBlocks.CARBON_PILE_COIL.has(level.getBlockState(current));
        if(!hasBase) {
            // Deconstruct
            current = current.above();
            while(level.getBlockState(current).is(this)) {
                level.setBlock(current, Blocks.COAL_BLOCK.defaultBlockState(), UPDATE_ALL);
            }
            return;
        }
        var base = current;
        if(state.getValue(TOP)) {
            // Try to extend the pile
            current = pos.above();
            var baseState = defaultBlockState();
            while(level.getBlockState(current).is(ModdedTags.Block.CARBON_PILE_BLOCK.tag) && size < CarbonPileCoilBlock.maxSize()) {
                ++size;
                level.setBlock(current, baseState.setValue(TOP, true), UPDATE_ALL);
                level.setBlock(current.below(), baseState.setValue(TOP, false), UPDATE_ALL);
            }
        } else {
            // Check if there is a pile above
            if(!level.getBlockState(pos.above()).is(this)) {
                level.setBlock(pos, state.setValue(TOP, true), UPDATE_ALL);
            }
        }
        level.getBlockEntity(base, ModdedBlockEntities.CARBON_PILE_COIL.get())
                .ifPresent(CarbonPileCoilBlockEntity::pileChanged);
    }
}
