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
package org.patryk3211.powergrid.kinetics.base;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public class HalfShaftVisual<T extends KineticBlockEntity> extends SingleAxisRotatingVisual<T> {
    public static Direction pickDir(BlockState state) {
        Direction dir = state.getOrEmpty(Properties.FACING)
                .or(() -> state.getOrEmpty(Properties.HORIZONTAL_FACING))
                .orElse(Direction.UP);
        return Direction.from(Direction.Axis.Y, dir.getDirection());
    }

    public HalfShaftVisual(VisualizationContext context, T blockEntity, float partialTick) {
        // This is a hacky way to flip the model to the correct facing.
        super(context, blockEntity, partialTick, pickDir(blockEntity.getCachedState()), Models.partial(AllPartialModels.SHAFT_HALF));
    }
}
