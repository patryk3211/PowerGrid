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

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS;
import static org.patryk3211.powergrid.kinetics.generator.rotor.RotorRenderer.getRotorAngle;

public class RotorVisual<T extends RotorBlockEntity> extends AbstractBlockEntityVisual<T> implements SimpleDynamicVisual {
    protected TransformedInstance assembly;

    public RotorVisual(VisualizationContext ctx, T blockEntity, float partialTick) {
        this(ctx, blockEntity, partialTick, Models.block(blockEntity.getBlockState()));
    }

    public RotorVisual(VisualizationContext ctx, T blockEntity, float partialTick, Model model) {
        super(ctx, blockEntity, partialTick);
        assembly = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, model)
                .createInstance()
                .rotateToFace(Direction.fromAxisAndDirection(getRotationAxis(), Direction.AxisDirection.POSITIVE));

        transformAssembly();
    }

    public Direction.Axis getRotationAxis() {
        if(blockState.hasProperty(AXIS))
            return blockState.getValue(AXIS);
        if(blockState.hasProperty(HORIZONTAL_AXIS))
            return blockState.getValue(HORIZONTAL_AXIS);
        return Direction.Axis.X;
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(assembly);
    }

    @Override
    public void updateLight(float v) {
        relight(assembly);
    }

    @Override
    protected void _delete() {
        assembly.delete();
    }

    @Override
    public void beginFrame(Context context) {
        transformAssembly();
    }

    public void transformAssembly() {
        var partial = AnimationTickHolder.getPartialTicks();
        var rotorAngle = getRotorAngle(blockEntity, partial);

        var dir = Direction.fromAxisAndDirection(getRotationAxis(), Direction.AxisDirection.POSITIVE);
        assembly.setIdentityTransform()
                .translate(getVisualPosition())
                .center()
                .rotate(rotorAngle, dir)
                .uncenter();
    }

//    protected Instancer<ModelData> getModel(BlockState state) {
//        return getAssemblyMaterial().getModel(state);
//    }
}
