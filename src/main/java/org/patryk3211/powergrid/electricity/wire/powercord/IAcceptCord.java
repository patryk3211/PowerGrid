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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public interface IAcceptCord {
    default boolean renderPlug() {
        return false;
    }

    @Nullable
    default AutoCordEndpoint getEndpoint(UseOnContext context) {
        if(!renderPlug()) {
            return new AutoCordEndpoint(context.getClickedPos(), 0, 1, context.getClickLocation(), null);
        } else {
            var facing = context.getClickedFace();
            var loc = context.getClickLocation();
            loc = loc.relative(facing, 3 / 16f);
            return new AutoCordEndpoint(context.getClickedPos(), 0, 1, loc, facing);
        }
    }

    @Nullable
    default ITerminalPlacement cordTerminal(BlockState state, Level level, BlockHitResult hit) {
        if(!renderPlug()) {
            var shape = state.getShape(level, hit.getBlockPos());
            var bb = shape.bounds();
            return new TerminalBoundingBox(IDecoratedTerminal.CORD, bb);
        } else {
            var facing = hit.getDirection();
            var loc = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos())).scale(16);
            var size = switch(facing.getAxis()) {
                case X -> new Vec3(1, 1.5, 1.5);
                case Y -> new Vec3(1.5, 1, 1.5);
                case Z -> new Vec3(1.5, 1.5, 1);
            };
            return new TerminalBoundingBox(IDecoratedTerminal.CORD,
                    loc.x - size.x, loc.y - size.y, loc.z - size.z,
                    loc.x + size.x, loc.y + size.y, loc.z + size.z);
        }
    }

    class Handler implements ICordPlacementHandler {
        @NotNull
        @Override
        public InteractionResultHolder<ICordEndpoint> place(BlockState state, UseOnContext context) {
            if(state.getBlock() instanceof IAcceptCord cordAcceptor) {
                var endpoint = cordAcceptor.getEndpoint(context);
                if(endpoint != null) {
                    return InteractionResultHolder.success(endpoint);
                }
            }
            return InteractionResultHolder.pass(null);
        }

        @Override
        public @Nullable ITerminalPlacement terminal(BlockState state, Level level, BlockHitResult hit) {
            if(state.getBlock() instanceof IAcceptCord cordAcceptor) {
                return cordAcceptor.cordTerminal(state, level, hit);
            }
            return null;
        }
    }
}
