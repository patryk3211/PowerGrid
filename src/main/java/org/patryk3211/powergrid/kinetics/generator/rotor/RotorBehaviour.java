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

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.SegmentedBehaviour;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.kinetics.generator.IRotorAssemblyPart;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RotorBehaviour extends SegmentedBehaviour<RotorBehaviour> {
    public static final BehaviourType<RotorBehaviour> TYPE = new BehaviourType<>("generator_rotor");
    private static final float ROTOR_INERTIA = 0.1f;

    // Energy values get loaded from NBT.
    protected float angularVelocity = 0;
    private float fieldStrength = 0.3f;

    // Segment count and inertia get calculated from added segments every time.
    private float inertia = 0;
    private int segmentCount = 0;

    // Angle is only for rendering and doesn't have to be saved.
    private float angle = 0;

    private boolean emitsField = true;
    private boolean hasSoundSource = false;

    public RotorBehaviour(SmartBlockEntity be) {
        super(be, ModdedConfigs.server().kinetics.rotorAssemblyMaxSize.get());
    }

    public void noField() {
        emitsField = false;
    }

    public boolean hasField() {
        return emitsField;
    }

    @Override
    protected List<RotorBehaviour> getConnected() {
        var world = getWorld();
        assert world != null;

        var state = blockEntity.getBlockState();
        if(!(state.getBlock() instanceof IRotorAssemblyPart assembly))
            return List.of();
        var pos = getPos();

        var checkDirs = new ArrayList<Direction>();
        for(var dir : Direction.values()) {
            if(assembly.canConnect(state, dir))
                checkDirs.add(dir);
        }
        List<RotorBehaviour> entities = new LinkedList<>();
        for(var dir : checkDirs) {
            var rotor = get(world, pos.relative(dir), getType());
            if(rotor == null)
                continue;
            var otherState = rotor.blockEntity.getBlockState();
            if(otherState.getBlock() instanceof IRotorAssemblyPart assemblyPart) {
                if(!assemblyPart.canConnect(otherState, dir.getOpposite()))
                    continue;
                entities.add(rotor);
            }
        }
        return entities;
    }

    @Override
    public BehaviourType<RotorBehaviour> getType() {
        return TYPE;
    }

    @Nullable
    public Direction.Axis getAxis() {
        var state = blockEntity.getBlockState();
        if(state.hasProperty(AbstractRotorBlock.AXIS))
            return state.getValue(AbstractRotorBlock.AXIS);
        return null;
    }

    @Override
    protected void makeController() {
        super.makeController();
        inertia = ROTOR_INERTIA;
        segmentCount = 1;
    }

    @Environment(EnvType.CLIENT)
    public void tickAudio() {
        if(!isController())
            return;
        if(!hasSoundSource && Math.abs(angularVelocity) > 32) {
            Minecraft.getInstance().getSoundManager().play(new RotorSoundInstance(this));
            hasSoundSource = true;
        } else if(hasSoundSource && Math.abs(angularVelocity) < 32) {
            hasSoundSource = false;
        }
    }

    @Override
    public void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if(compound.contains("AngularVelocity")) {
            angularVelocity = compound.getFloat("AngularVelocity");
            if(Float.isNaN(angularVelocity))
                angularVelocity = 0;
        }
        if(compound.contains("FieldStrength")) {
            fieldStrength = compound.getFloat("FieldStrength");
        }
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putFloat("AngularVelocity", angularVelocity);
        compound.putFloat("FieldStrength", fieldStrength);
    }

    @Override
    public void readController(CompoundTag compound, boolean clientPacket) {
    }

    @Override
    public void writeController(CompoundTag compound, boolean clientPacket) {
    }

    @Override
    public void segmentAdded(RotorBehaviour segment) {
        super.segmentAdded(segment);
        var momentum = angularVelocity * inertia + segment.angularVelocity * ROTOR_INERTIA;
        inertia += ROTOR_INERTIA;
        angularVelocity = momentum / inertia;
        segmentCount += 1;
    }

    @Override
    public void segmentRemoved(RotorBehaviour segment) {
        super.segmentRemoved(segment);
        inertia -= ROTOR_INERTIA;
        segmentCount -= 1;
    }

    public void applyTickForce(float force) {
        var controller = getControllerOrThis();
        if(Math.abs(force) > 0.001f) {
            controller.angularVelocity += force / controller.inertia / 20f;
            if(Float.isNaN(controller.angularVelocity))
                controller.angularVelocity = 0;
        }
    }

    public float limitForce(float forceIn) {
        var controller = getControllerOrThis();
        if((forceIn > 0 && controller.angularVelocity > 0) || (forceIn < 0 && controller.angularVelocity < 0)) {
            // Matching signs, no change.
            return forceIn;
        }
        // Max force is the force that stops the rotor.
        var maxForce = controller.angularVelocity * 20f * controller.inertia;
        return Math.signum(forceIn) * Math.min(Math.abs(forceIn), Math.abs(maxForce));
    }

    /**
     * Get the rotor angular velocity.
     * @return Angular velocity in rotations per minute.
     */
    public float getAngularVelocity() {
        var controller = getControllerOrThis();
        return controller.angularVelocity;
    }

    /**
     * Get the rotor angular velocity
     * @return Angular velocity in radians per second.
     */
    public float getAngularVelocityRadians() {
        return 2f * getAngularVelocity() * (float) Math.PI / 60f;
    }

    public float getInertia() {
        var controller = getControllerOrThis();
        return controller.inertia;
    }

    public float getAngle() {
        var controller = getControllerOrThis();
        return controller.angle;
    }

    public float getFieldStrength() {
        if(!emitsField)
            return 0;
        return fieldStrength;
    }

    public void setFieldStrength(float value) {
        fieldStrength = value;
        blockEntity.setChanged();
    }

    @Override
    public void tick() {
        super.tick();
        if(isController()) {
            var velocity = getAngularVelocity();

            float friction = Math.abs(velocity * 20f * inertia);
            friction = Math.min(friction, segmentCount * 1f);

            angularVelocity -= Math.signum(velocity) * friction / 20f / inertia;
            if(Math.abs(angularVelocity) < 0.01 || Float.isNaN(angularVelocity))
                angularVelocity = 0;

            if(Math.abs(angularVelocity) > 320 && !getWorld().isClientSide) {
                // TODO: Maybe make this a bit more destructive.
                getWorld().destroyBlock(getPos(), false);
            }

            angle = (angle + velocity * 0.3f) % 360;
            if(Float.isNaN(angle))
                angle = 0;

            if(getWorld() != null && getWorld().isClientSide)
                tickAudio();
        } else {
            // Fetch values from controller
            angularVelocity = getAngularVelocity();
            angle = getAngle();
        }
        blockEntity.setChanged();
    }
}
