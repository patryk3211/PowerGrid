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
package org.patryk3211.powergrid.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public interface IConnectableBlock {
    boolean connects(BlockState state, Direction side, BlockState checkState);
    boolean canPropagate(BlockState state, Direction direction);

    static List<BlockPos> gatherBlocks(Level world, BlockPos firstPos) {
        List<BlockPos> allBlocks = new ArrayList<>();
        List<BlockPos> visitQueue = new ArrayList<>();

        visitQueue.add(firstPos);
        while(!visitQueue.isEmpty()) {
            var pos = visitQueue.remove(0);

            var state = world.getBlockState(pos);
            if(!(state.getBlock() instanceof IConnectableBlock connectable))
                continue;

            allBlocks.add(pos);
            for(var dir : Direction.values()) {
                if(!connectable.canPropagate(state, dir))
                    continue;
                var neighborPos = pos.relative(dir);
                if(allBlocks.contains(neighborPos))
                    continue;
                var neighbor = world.getBlockState(neighborPos);
                if(connectable.connects(state, dir, neighbor))
                    visitQueue.add(neighborPos);
            }
        }

        return allBlocks;
    }
}
