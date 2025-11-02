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
package org.patryk3211.powergrid.utility.fabric;

import io.github.fabricators_of_create.porting_lib.event.common.BlockEvents;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PlayerUtilitiesImpl {
    public static boolean isFake(Player player) {
        return player instanceof FakePlayer;
    }

    public static boolean cancelBreak(Level world, BlockPos pos, Player player) {
        var event = new BlockEvents.BreakEvent(world, pos, world.getBlockState(pos), player);
        BlockEvents.BLOCK_BREAK.invoker().onBlockBreak(event);
        return event.isCanceled();
    }
}
