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

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.wire.powercord.ICordEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.ICordPlacementHandler;
import org.patryk3211.powergrid.electricity.wire.powercord.SocketEndpoint;
import org.patryk3211.powergrid.utility.Lang;

public interface ISocketElectric {
    static ISocketElectric getAt(Level world, BlockPos pos) {
        var state = world.getBlockState(pos);
        if(state.getBlock() instanceof ISocketElectric electric)
            return electric;
        return null;
    }

    ITerminalPlacement socket(BlockState state);

    class Handler implements ICordPlacementHandler {
        @Override
        public @NotNull InteractionResultHolder<ICordEndpoint> place(BlockState state, UseOnContext context) {
            var socket = ISocketElectric.getAt(context.getLevel(), context.getClickedPos());
            if(socket != null) {
                var terminal = socket.socket(state);
                if(terminal.check(context.getClickedPos(), context.getClickLocation())) {
                    var endpoint = new SocketEndpoint(context.getClickedPos());
                    if(endpoint.hasConnection(context.getLevel())) {
                        IElectric.sendMessage(context, Lang.translate("message.socket_taken").style(ChatFormatting.RED).component());
                        return InteractionResultHolder.fail(null);
                    } else {
                        return InteractionResultHolder.success(endpoint);
                    }
                }
            }
            return InteractionResultHolder.pass(null);
        }

        @Override
        public @Nullable ITerminalPlacement terminal(BlockState state, Level level, BlockHitResult hit) {
            var socket = ISocketElectric.getAt(level, hit.getBlockPos());
            if(socket != null) {
                var terminal = socket.socket(state);
                if(terminal.check(hit.getBlockPos(), hit.getLocation()))
                    return terminal;
            }
            return null;
        }
    }
}
