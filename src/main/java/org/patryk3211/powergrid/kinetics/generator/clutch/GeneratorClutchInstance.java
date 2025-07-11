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

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.simibubi.create.content.kinetics.base.ShaftInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

import static net.minecraft.state.property.Properties.FACING;
import static org.patryk3211.powergrid.kinetics.generator.rotor.RotorRenderer.getRotorAngle;

public class GeneratorClutchInstance extends ShaftInstance<GeneratorClutchBlockEntity> implements DynamicInstance {
    protected ModelData assembly;

    public GeneratorClutchInstance(MaterialManager materialManager, GeneratorClutchBlockEntity blockEntity) {
        super(materialManager, blockEntity);
        assembly = materialManager.defaultSolid()
                .material(Materials.TRANSFORMED)
                .getModel(ModdedPartialModels.CLUTCH_SHAFT, blockState, blockState.get(FACING).getOpposite())
                .createInstance();
        transformAssembly();
    }

    @Override
    protected Instancer<RotatingData> getModel() {
        return getRotatingMaterial().getModel(ModdedPartialModels.SHAFT_BIT, blockState, blockState.get(FACING));
    }

    public void transformAssembly() {
        var partial = AnimationTickHolder.getPartialTicks();
        var rotorAngle = getRotorAngle(blockEntity, partial);

        var dir = Direction.from(blockState.get(GeneratorClutchBlock.FACING).getAxis(), Direction.AxisDirection.POSITIVE);
        assembly.loadIdentity()
                .translate(getInstancePosition())
                .centre()
                .rotate(dir, rotorAngle)
                .unCentre();
    }

    @Override
    public void beginFrame() {
        transformAssembly();
    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, assembly);
    }

    @Override
    public void remove() {
        super.remove();
        assembly.delete();
    }
}
