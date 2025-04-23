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
import net.minecraft.nbt.NbtCompound;
import org.patryk3211.powergrid.base.SegmentedBehaviour;

import java.util.LinkedList;
import java.util.List;

public class RotorBehaviour extends SegmentedBehaviour {
    public static final BehaviourType<RotorBehaviour> TYPE = new BehaviourType<>("rotor");
    private static final float ROTOR_INERTIA = 0.1f;

    // Energy values get loaded from NBT.
    protected float angularVelocity = 0;
    private float fieldStrength = 0.3f;

    private float prevAngularVelocity = 0;

    // Segment count and inertia get calculated from added segments every time.
    private float inertia = 0;
    private int segmentCount = 0;

    // Angle is only for rendering and doesn't have to be saved.
    private float angle = 0;

    public RotorBehaviour(SmartBlockEntity be) {
        super(be);
    }

    @Override
    protected List<RotorBehaviour> getConnected() {
        var world = getWorld();
        assert world != null;

        var state = blockEntity.getCachedState();
        var pos = getPos();

        var axis = state.get(RotorBlock.AXIS);
        var direction = state.get(RotorBlock.SHAFT_DIRECTION);
        List<RotorBehaviour> entities = new LinkedList<>();

        RotorBehaviour positive = null;
        RotorBehaviour negative = null;
        switch(axis) {
            case X -> {
                positive = get(world, pos.east(), getType());
                negative = get(world, pos.west(), getType());
            }
            case Y -> {
                positive = get(world, pos.up(), getType());
                negative = get(world, pos.down(), getType());
            }
            case Z -> {
                positive = get(world, pos.south(), getType());
                negative = get(world, pos.north(), getType());
            }
        }
        switch(direction) {
            case NONE -> {
                if(positive != null) {
                    var otherState = positive.blockEntity.getCachedState();
                    if(otherState.get(RotorBlock.AXIS) == axis && otherState.get(RotorBlock.SHAFT_DIRECTION) != ShaftDirection.NEGATIVE) {
                        entities.add(positive);
                    }
                }
                if(negative != null) {
                    var otherState = negative.blockEntity.getCachedState();
                    if(otherState.get(RotorBlock.AXIS) == axis && otherState.get(RotorBlock.SHAFT_DIRECTION) != ShaftDirection.POSITIVE) {
                        entities.add(negative);
                    }
                }
            }
            case POSITIVE -> {
                if(negative != null) {
                    var otherState = negative.blockEntity.getCachedState();
                    if(otherState.get(RotorBlock.AXIS) == axis && otherState.get(RotorBlock.SHAFT_DIRECTION) != ShaftDirection.POSITIVE) {
                        entities.add(negative);
                    }
                }
            }
            case NEGATIVE -> {
                if(positive != null) {
                    var otherState = positive.blockEntity.getCachedState();
                    if(otherState.get(RotorBlock.AXIS) == axis && otherState.get(RotorBlock.SHAFT_DIRECTION) != ShaftDirection.NEGATIVE) {
                        entities.add(positive);
                    }
                }
            }
        }
        return entities;
    }

    @Override
    public BehaviourType<RotorBehaviour> getType() {
        return TYPE;
    }

    @Override
    protected void makeController() {
        super.makeController();
        inertia = ROTOR_INERTIA;
        segmentCount = 1;
        angularVelocity = 0;
        angle = 0;
    }

    @Override
    public void readController(NbtCompound compound, boolean clientPacket) {
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
    public void writeController(NbtCompound compound, boolean clientPacket) {
        compound.putFloat("AngularVelocity", angularVelocity);
        compound.putFloat("FieldStrength", fieldStrength);
    }

    @Override
    public void segmentAdded(SegmentedBehaviour behaviour) {
        super.segmentAdded(behaviour);
        // Treat the new segment as it having 0 velocity.
        angularVelocity *= (float) Math.sqrt(inertia / (inertia + ROTOR_INERTIA));
        inertia += ROTOR_INERTIA;
        segmentCount += 1;
    }

    @Override
    public void segmentRemoved(SegmentedBehaviour behaviour) {
        super.segmentRemoved(behaviour);
        inertia -= ROTOR_INERTIA;
        segmentCount -= 1;
    }

    public void applyTickForce(float force) {
        var controller = (RotorBehaviour) getControllerOrThis();
        if(controller != null && Math.abs(force) > 0.001f) {
            angularVelocity += force / inertia / 20f;
            if(Float.isNaN(angularVelocity))
                angularVelocity = 0;
        }
    }

    /**
     * Get the rotor angular velocity.
     * @return Angular velocity in rotations per minute.
     */
    public float getAngularVelocity() {
        var controller = (RotorBehaviour) getControllerOrThis();
        if(controller != null)
            return (controller.angularVelocity + controller.prevAngularVelocity) / 2f;
        return 0;
    }

    /**
     * Get the rotor angular velocity
     * @return Angular velocity in radians per second.
     */
    public float getAngularVelocityRadians() {
        return 2f * getAngularVelocity() * (float) Math.PI / 60f;
    }

    public float getInertia() {
        var controller = (RotorBehaviour) getControllerOrThis();
        if(controller != null)
            return controller.inertia;
        return 0;
    }

    public float getAngle() {
        var controller = (RotorBehaviour) getControllerOrThis();
        if(controller != null)
            return controller.angle;
        return 0;
    }

    public float getFieldStrength() {
        var controller = (RotorBehaviour) getControllerOrThis();
        if(controller != null)
            return controller.fieldStrength;
        return 0;
    }

    public void setFieldStrength(float value) {
        var controller = (RotorBehaviour) getControllerOrThis();
        if(controller != null) {
            controller.fieldStrength = value;
            controller.blockEntity.markDirty();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(isController()) {
            var velocity = getAngularVelocity();
            prevAngularVelocity = angularVelocity;

            float friction = Math.abs(velocity * 20f * inertia);
            friction = Math.min(friction, segmentCount * 1f);
            angularVelocity -= Math.signum(velocity) * friction / 20f / inertia;

            angle = (angle + velocity * 0.3f) % 360;

            if(Math.abs(angularVelocity) < 0.01)
                angularVelocity = 0;

            blockEntity.markDirty();
        }
    }
}
