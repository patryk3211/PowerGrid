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
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.kinetics.generator.IRotorAssemblyPart;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RotorBehaviour extends SegmentedBehaviour<RotorBehaviour> {
    public static final BehaviourType<RotorBehaviour> TYPE = new BehaviourType<>("generator_rotor");
    private static final int OVERSPEED_TICKS = 5;

    // Energy values get loaded from NBT.
    protected float totalForce = 0;
    protected float angularVelocity = 0;
    private float fieldStrength = 0.3f;
    private final float individualInertia;

    // Segment count and inertia get calculated from added segments every time.
    private float inertia = 0;
    private int segmentCount = 0;
<<<<<<< HEAD

	/* This is the OLD Angular Velocity; by keeping track of its value over
=======
	
	/* This is the OLD Angular Velocity; by keeping track of its value over 
>>>>>>> parent of b6761279 (Added decorative wire)
	time, we might be able to perform derivative calculations */
	private float oldAngVel = 0;

    // Angle is only for rendering and doesn't have to be saved.
    private float angle = 0;
    private int overspeedTicks = 0;

    private boolean emitsField = true;
    private boolean hasSoundSource = false;
    private IForceSource forceSupplier = null;

    public RotorBehaviour(SmartBlockEntity be, float inertia) {
        super(be, ModdedConfigs.server().kinetics.rotorAssemblyMaxSize.get(),
                segment -> !segment.blockEntity.getBlockState()
                        .is(ModdedTags.Block.IGNORE_IN_ROTOR_ASSEMBLY_SIZE.tag));
        this.individualInertia = inertia;
        setLazyTickRate(100);
    }

    public void noField() {
        emitsField = false;
    }

    public boolean hasField() {
        return emitsField;
    }

    public void forceSource(IForceSource availableForce) {
        this.forceSupplier = availableForce;
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
        inertia = individualInertia;
        segmentCount = 1;
		oldAngVel = 0;
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
        var momentum = angularVelocity * inertia + segment.angularVelocity * segment.individualInertia;
        inertia += segment.individualInertia;
        angularVelocity = momentum / inertia;
        segmentCount += 1;
    }

    @Override
    public void segmentRemoved(RotorBehaviour segment) {
        super.segmentRemoved(segment);
        inertia -= segment.individualInertia;
        segmentCount -= 1;
    }

    public void applyTickForce(float force) {
        var controller = getControllerOrThis();
        if(controller.inertia <= 0) {
            // Not initialized yet (most likely)
            return;
        }
        if(Math.abs(force) > 0.001f) {
            controller.totalForce += force;
//            controller.angularVelocity += force / controller.inertia / 20f;
//            if(Float.isNaN(controller.angularVelocity))
//                controller.angularVelocity = 0;
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
<<<<<<< HEAD
     */
=======
     */   
>>>>>>> parent of b6761279 (Added decorative wire)
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

    public static int getMaxRotationSpeed() {
        return ModdedConfigs.server().kinetics.rotorRPMMax.get();
    }
	/* Get the Proportional constant for calculations */
	public static float getRotorKp() {
		return ModdedConfigs.server().kinetics.propDiffControl.rotorKp.getF();
	}
	/* And the Derivative, for good measure */
	public static float getRotorKd() {
		return ModdedConfigs.server().kinetics.propDiffControl.rotorKd.getF();
	}

    public void setFieldStrength(float value) {
        fieldStrength = value;
        blockEntity.setChanged();
    }
<<<<<<< HEAD

=======
	
>>>>>>> parent of b6761279 (Added decorative wire)
	/* Sets the old AngVel amd sends the change to the block entity */
	public void setOldAngVel(float value) {
		oldAngVel = value;
		blockEntity.setChanged();
	}
<<<<<<< HEAD

=======
	
>>>>>>> parent of b6761279 (Added decorative wire)
	/* Of course, we'll need to get the old AngVel, too, if we want to do
	derivative calculations. */
	public float getOldAngVel() {
		var controller = getControllerOrThis();
		return controller.oldAngVel;
	}

    @Override
    public void lazyTick() {
        super.lazyTick();
        blockEntity.sendData();
    }
<<<<<<< HEAD

=======
	
>>>>>>> parent of b6761279 (Added decorative wire)
    @Override
    public void tick() {
        super.tick();
        if(isController()) {
			/* Get the old and current Angular Velocity */
			var oldAV = getOldAngVel();
            var velocity = getAngularVelocity();

            float friction = Math.abs(velocity * 20f * inertia);
            friction = Math.min(friction, segmentCount * 1f);
            totalForce -= Math.signum(velocity) * friction;

            angularVelocity += totalForce / 20f / inertia;
            if(Math.abs(angularVelocity) < 0.01 || Float.isNaN(angularVelocity))
                angularVelocity = 0;
            totalForce = 0;

            forEachSegment(segment -> {
                if(segment.forceSupplier != null) {
                    var target = segment.forceSupplier.forceSpeed();
					/* Get the Kp and Kd factors for the equation coming up */
					float Kp = getRotorKp();
					float Kd = getRotorKd();
					/* Get the current field strength, so we can scale things
					properly */
					float fieldStrength = getFieldStrength();
<<<<<<< HEAD
					/* Delta T is the diff between Target and AngVel
=======
					/* Delta T is the diff between Target and AngVel 
>>>>>>> parent of b6761279 (Added decorative wire)
					(Analogous to Proportional control in PID) */
                    float deltaT = (target - angularVelocity);
                    if(target < 0)
                        deltaT = -deltaT;
                    deltaT = Math.max(0, deltaT);
<<<<<<< HEAD
					/* Delta A.V. is the change in AngVel since last tick
=======
					/* Delta A.V. is the change in AngVel since last tick 
>>>>>>> parent of b6761279 (Added decorative wire)
					(Analogous to Derivative/Differential in PID) */
					float deltaAV = oldAV - angularVelocity;
					/* Maybe we can scale Kp by the field strength? */
					float Kds = fieldStrength * Kd;
                    float maxForce = segment.forceSupplier.sourceForce();
                    //float force = deltaT * 20f * inertia;
					/* Calculate with PD instead of simply P */
                    float force = ((Kp * deltaT) + (Kds * deltaAV)) * 20f * inertia;
					force = Math.min(Math.abs(force), maxForce) * Math.signum(target);
                    angularVelocity += force / 20f / inertia;
                    segment.forceSupplier.receiveUsedForce(force / maxForce);
                }
            });

            if(!getWorld().isClientSide) {
                var V = Math.abs(angularVelocity);
                var Vmax = getMaxRotationSpeed();
                if(V > Vmax) {
                    if(overspeedTicks++ >= OVERSPEED_TICKS) {
                        // Overspeed for too long
                        getWorld().destroyBlock(getPos(), false);
                        checkConnectivity(this);
                    } else {
                        // Clamp to max speed, if something is still accelerating it, the rotor will be destroyed
                        angularVelocity = Vmax * Math.signum(angularVelocity);
                        blockEntity.sendData();
                    }
                } else if(V <= Vmax) {
                    overspeedTicks = 0;
                }
            }

            angle = (angle + velocity * 0.3f) % 360;
            if(Float.isNaN(angle))
                angle = 0;

            if(getWorld() != null && getWorld().isClientSide)
                tickAudio();
			/* Set today's AngVel to be tomorrow's oldAngVel */
			setOldAngVel(angularVelocity);
        } else {
            // Fetch values from controller
            angularVelocity = getAngularVelocity();
            angle = getAngle();
        }
        blockEntity.setChanged();
    }

    public interface IForceSource {
        float sourceForce();
        float forceSpeed();
        default void receiveUsedForce(float percent) { }
    }
}
