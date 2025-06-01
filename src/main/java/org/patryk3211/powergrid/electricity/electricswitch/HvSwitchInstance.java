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

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.simibubi.create.content.kinetics.base.ShaftInstance;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class HvSwitchInstance extends ShaftInstance<HvSwitchBlockEntity> implements DynamicInstance {
    protected ModelData pointer;
    protected boolean settled;

    protected final double yRot;

    public HvSwitchInstance(MaterialManager materialManager, HvSwitchBlockEntity blockEntity) {
        super(materialManager, blockEntity);
        var facing = blockState.get(HvSwitchBlock.HORIZONTAL_FACING);

        yRot = AngleHelper.horizontalAngle(facing);
        settled = false;

        pointer = materialManager.defaultSolid()
                .material(Materials.TRANSFORMED)
                .getModel(ModdedPartialModels.HV_SWITCH_ROD, blockState).createInstance();

        transformRod();
    }

    public void transformRod() {
        float value = blockEntity.rod.getValue(AnimationTickHolder.getPartialTicks());
//        float rotation = MathHelper.lerp(value, 90, 0);
        float rotation = (1.0f - value) * -90f;
        settled = (value == 0 || value == 1) && blockEntity.rod.settled();

        pointer.loadIdentity()
                .translate(getInstancePosition())
                .centre()
                .rotateY(yRot)
                .rotateX(rotation)
                .unCentre();
    }

    @Override
    public void beginFrame() {
        if(blockEntity.rod.settled() && settled)
            return;

        transformRod();
    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, pointer);
    }

    @Override
    public void remove() {
        super.remove();
        pointer.delete();
    }
}
