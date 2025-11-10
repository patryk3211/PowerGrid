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
package org.patryk3211.powergrid.utility;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * This copied from Fabric Create since it's not available in Forge Create
 * @see com.simibubi.create.infrastructure.fabric.SimpleEntityVisualFactory
 */
@FunctionalInterface
public interface SimpleBlockEntityVisualFactory<T extends BlockEntity> {
    BlockEntityVisual<? super T> create(VisualizationContext ctx, T entity, float partialTicks);
}
