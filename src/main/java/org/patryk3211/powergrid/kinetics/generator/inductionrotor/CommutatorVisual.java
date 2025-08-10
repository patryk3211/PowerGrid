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
package org.patryk3211.powergrid.kinetics.generator.inductionrotor;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorVisual;

public class CommutatorVisual extends RotorVisual<CommutatorBlockEntity> {
    public CommutatorVisual(VisualizationContext ctx, CommutatorBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick, Models.partial(ModdedPartialModels.COMMUTATOR_SHAFT));
    }

//    @Override
//    protected Instancer<ModelData> getModel(BlockState state) {
//        var facing = Direction.from(state.get(CommutatorBlock.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE);
//        return getAssemblyMaterial().getModel(ModdedPartialModels.COMMUTATOR_SHAFT, state, facing);
//    }
}
