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
package org.patryk3211.powergrid.electricity.electricswitch;

import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class HvSwitchVisual extends ShaftVisual<HvSwitchBlockEntity> implements SimpleDynamicVisual {
    protected TransformedInstance pointer;
    protected boolean settled;

    protected final float yRot;

    public HvSwitchVisual(VisualizationContext context, HvSwitchBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var facing = blockState.get(HvSwitchBlock.HORIZONTAL_FACING);

        yRot = AngleHelper.horizontalAngle(facing);
        settled = false;

        pointer = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(ModdedPartialModels.HV_SWITCH_ROD))
                .createInstance();

        transformRod();
    }

    public void transformRod() {
        float value = blockEntity.rod.getValue(AnimationTickHolder.getPartialTicks());
        float rotation = (1.0f - value) * -90f;
        settled = (value == 0 || value == 1) && blockEntity.rod.settled();

        pointer.setIdentityTransform()
                .translate(getVisualPosition())
                .center()
                .rotateYDegrees(yRot)
                .rotateXDegrees(rotation)
                .uncenter();
    }

    @Override
    public void beginFrame(Context context) {
        if(blockEntity.rod.settled() && settled)
            return;

        transformRod();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(pointer);
    }

    @Override
    protected void _delete() {
        super._delete();
        pointer.delete();
    }
}
