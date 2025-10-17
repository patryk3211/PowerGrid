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
package org.patryk3211.powergrid.electricity.wire.powercord;

import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;

public interface ICordPlacementHandler {
    /**
     * Called when cord item is used to try and place a cord.
     * @param state Block state at the clicked position
     * @param context Use context
     * @return Interaction result, when PASS is returned, other handlers can get called.
     * Any other result consumes the interaction. Only when the action is successful, the created
     * endpoint is used to place down a cord, in all other cases, the value is ignored.
     */
    @NotNull
    InteractionResultHolder<ICordEndpoint> place(BlockState state, UseOnContext context);

    /**
     * Called to get a visual attachment point that will be displayed as a placement hint to the client
     * @param state Target block state
     * @param level Client level
     * @param hit Block hit result
     * @return Terminal to render
     */
    @Nullable
    ITerminalPlacement terminal(BlockState state, Level level, BlockHitResult hit);
}
