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
package org.patryk3211.powergrid.kinetics.generator.clutch;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

import static net.minecraft.state.property.Properties.FACING;
import static org.patryk3211.powergrid.kinetics.generator.rotor.RotorRenderer.getRotorAngle;

public class GeneratorClutchInstance extends SingleAxisRotatingVisual<GeneratorClutchBlockEntity> implements SimpleDynamicVisual {
    protected TransformedInstance assembly;

    public GeneratorClutchInstance(VisualizationContext context, GeneratorClutchBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick, Models.partial(ModdedPartialModels.SHAFT_BIT));
        assembly = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(ModdedPartialModels.CLUTCH_SHAFT))
                .createInstance()
                .rotateToFace(blockState.get(FACING).getOpposite());
        transformAssembly();
    }

    public void transformAssembly() {
        var partial = AnimationTickHolder.getPartialTicks();
        var rotorAngle = getRotorAngle(blockEntity, partial);

        var dir = Direction.from(blockState.get(GeneratorClutchBlock.FACING).getAxis(), Direction.AxisDirection.POSITIVE);
        assembly.setIdentityTransform()
                .translate(getVisualPosition())
                .center()
                .rotate(rotorAngle, dir)
                .uncenter();
    }

    @Override
    public void beginFrame(Context context) {
        transformAssembly();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(assembly);
    }

    @Override
    protected void _delete() {
        super._delete();
        assembly.delete();
    }
}
