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
package org.patryk3211.powergrid.kinetics.generator.rotor;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

import static net.minecraft.state.property.Properties.AXIS;
import static net.minecraft.state.property.Properties.HORIZONTAL_AXIS;
import static org.patryk3211.powergrid.kinetics.generator.rotor.RotorRenderer.getRotorAngle;

public class RotorInstance extends BlockEntityInstance<SmartBlockEntity> implements DynamicInstance {
    protected ModelData assembly;

    public RotorInstance(MaterialManager materialManager, SmartBlockEntity blockEntity) {
        super(materialManager, blockEntity);
        assembly = getModel(blockState).createInstance();
        transformAssembly();
    }

    @Override
    protected void remove() {
        assembly.delete();
    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, assembly);
    }

    @Override
    public void beginFrame() {
        transformAssembly();
    }

    public Direction.Axis getRotationAxis() {
        if(blockState.contains(AXIS))
            return blockState.get(AXIS);
        if(blockState.contains(HORIZONTAL_AXIS))
            return blockState.get(HORIZONTAL_AXIS);
        return Direction.Axis.X;
    }

    public void transformAssembly() {
        var partial = AnimationTickHolder.getPartialTicks();
        var rotorAngle = getRotorAngle(blockEntity, partial);

        var dir = Direction.from(getRotationAxis(), Direction.AxisDirection.POSITIVE);
        assembly.loadIdentity()
                .translate(getInstancePosition())
                .centre()
                .rotate(dir, rotorAngle)
                .unCentre();
    }

    protected Material<ModelData> getAssemblyMaterial() {
        return materialManager.defaultSolid().material(Materials.TRANSFORMED);
    }

    protected Instancer<ModelData> getModel(BlockState state) {
        return getAssemblyMaterial().getModel(state);
    }
}
