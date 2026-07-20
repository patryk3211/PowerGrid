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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.SegmentedBehaviour;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedDamageTypes;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.IRotor;
import org.patryk3211.powergrid.kinetics.generator.IRotorAssemblyPart;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.patryk3211.powergrid.kinetics.generator.rotor.AbstractRotorBlock.AXIS;

public class RotorBehaviour extends SegmentedBehaviour<RotorBehaviour> implements IRotor, ElectricBehaviour.SyncAppender {
    public static final BehaviourType<RotorBehaviour> TYPE = new BehaviourType<>("generator_rotor");
    private static final int OVERSPEED_TICKS = 5;

    // Energy values get loaded from NBT.
    protected float totalForce = 0;
    public float prevForce = 0;
    protected float angularVelocity = 0;
    private final float individualInertia;

    // Segment count and inertia get calculated from added segments every time.
    private float inertia = 0;
    private int segmentCount = 0;

	/* This is the OLD Angular Velocity; by keeping track of its value over 
	time, we might be able to perform derivative calculations */
	private float oldAngVel = 0;

    // Angle is only for rendering and doesn't have to be saved.
    private float angle = 0;
    private int overspeedTicks = 0;

    private boolean hasSoundSource = false;
    private IForceSource forceSupplier = null;

    public float power;

    private final float damageRadius;
    private AABB hurtBB;
    private BlockState bbState;
    private DamageSource dmgSource;

    public RotorBehaviour(SmartBlockEntity be, float inertia, float damageRadius) {
        super(be, ModdedConfigs.server().kinetics.generatorControls.rotorAssemblyMaxSize.get(),
                segment -> !segment.blockEntity.getBlockState()
                        .is(ModdedTags.Block.IGNORE_IN_ROTOR_ASSEMBLY_SIZE.tag));
        this.individualInertia = inertia;
        this.damageRadius = damageRadius;
        setLazyTickRate(100);
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
        if(state.hasProperty(AXIS))
            return state.getValue(AXIS);
        return null;
    }

    @Override
    protected void makeController() {
        super.makeController();
        inertia = individualInertia;
        segmentCount = 1;
		oldAngVel = 0;
    }

    @Override
    protected void makePeripheral(RotorBehaviour controller) {
        super.makePeripheral(controller);
        hasSoundSource = false;
    }

    @Environment(EnvType.CLIENT)
    public void tickAudio() {
        if(!isController()) {
            hasSoundSource = false;
            return;
        }
        if(!hasSoundSource && Math.abs(angularVelocity) > 32) {
            Minecraft.getInstance().getSoundManager().play(new RotorSoundInstance(this));
            hasSoundSource = true;
        } else if(hasSoundSource && Math.abs(angularVelocity) < 32) {
            hasSoundSource = false;
        }
    }

    @Override
    public void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if(compound.contains("AngularVelocity")) {
            angularVelocity = compound.getFloat("AngularVelocity");
            if(Float.isNaN(angularVelocity))
                angularVelocity = 0;
        }
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putFloat("AngularVelocity", angularVelocity);
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

    @Override
    public float getAngularVelocity() {
        var controller = getControllerOrThis();
        return controller.angularVelocity;
    }

    @Override
    public float getInertia() {
        var controller = getControllerOrThis();
        return controller.inertia;
    }

    public float getAngle() {
        var controller = getControllerOrThis();
        return controller.angle;
    }

    public static int getMaxRotationSpeed() {
        return ModdedConfigs.server().kinetics.generatorControls.rotorRPMMax.get();
    }
	/* Get the Proportional constant for calculations */
	public static float getRotorKp() {
		return ModdedConfigs.server().kinetics.generatorControls.rotorKp.getF();
	}
	/* And the Derivative, for good measure */
	public static float getRotorKd() {
		return ModdedConfigs.server().kinetics.generatorControls.rotorKd.getF();
	}

	/* Sets the old AngVel amd sends the change to the block entity */
	public void setOldAngVel(float value) {
		oldAngVel = value;
	}

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

    @Override
    public void tick() {
        super.tick();
        if(isController()) {
            /* Get the old and current Angular Velocity */
            var oldAV = getOldAngVel();
            var velocity = getAngularVelocity();

            float friction = Math.abs(velocity * 20f * inertia);
            friction = Math.min(friction, segmentCount * ModdedConfigs.server().kinetics.generatorControls.rotorSegmentFriction.getF());
            totalForce -= Math.signum(velocity) * friction;

            angularVelocity += totalForce / 20f / inertia;
            if(Math.abs(angularVelocity) < 0.01 || Float.isNaN(angularVelocity))
                angularVelocity = 0;

            forEachSegment(segment -> {
                if(segment.forceSupplier != null) {
                    var target = segment.forceSupplier.forceSpeed();
                    /* Get the Kp and Kd factors for the equation coming up */
                    float Kp = getRotorKp();
                    float Kd = getRotorKd();
                    /* Delta T is the diff between Target and AngVel
                    (Analogous to Proportional control in PID) */
                    float deltaT = (target - angularVelocity);
                    if(target < 0)
                        deltaT = -deltaT;
                    deltaT = Math.max(0, deltaT);
                    /* Delta A.V. is the change in AngVel since last tick 
                    (Analogous to Derivative/Differential in PID) */
                    float deltaAV = oldAV - angularVelocity;
                    /* Maybe we can scale Kp by the field strength? */
                    float maxForce = segment.forceSupplier.sourceForce(angularVelocity);
                    /* Calculate with PD instead of simply P */
                    float force = ((Kp * deltaT) + (Kd * deltaAV)) * 20f * inertia;
                    force = Math.min(Math.abs(force), maxForce) * Math.signum(target);
                    angularVelocity += force / 20f / inertia;
                    segment.forceSupplier.receiveUsedForce(Math.abs(force / maxForce));
                    totalForce += force;

                    power = force * getAngularVelocityRadians();
                }
            });
            prevForce = totalForce;
            totalForce = 0;

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
        getWorld().blockEntityChanged(getPos());
        damageCalc();
    }

    private void damageCalc() {
        if(damageRadius == 0 || !ModdedConfigs.server().kinetics.generatorControls.dangerousGenerators.get())
            return;
        var state = blockEntity.getBlockState();
        if(bbState != blockEntity.getBlockState()) {
            bbState = state;
            var axis = bbState.getValue(AXIS);
            var pos = getPos();
            hurtBB = new AABB(
                    axis == Direction.Axis.X ? pos.getX() : pos.getX() + 0.5 - damageRadius,
                    axis == Direction.Axis.Y ? pos.getY() : pos.getY() + 0.5 - damageRadius,
                    axis == Direction.Axis.Z ? pos.getZ() : pos.getZ() + 0.5 - damageRadius,
                    axis == Direction.Axis.X ? pos.getX() + 1 : pos.getX() + 0.5 + damageRadius,
                    axis == Direction.Axis.Y ? pos.getY() + 1 : pos.getY() + 0.5 + damageRadius,
                    axis == Direction.Axis.Z ? pos.getZ() + 1 : pos.getZ() + 0.5 + damageRadius
            );
        }
        var level = getWorld();
        if(dmgSource == null)
            dmgSource = ModdedDamageTypes.SPINNING_ROTOR.simpleDamageSource(level);
        var entities = level.getEntitiesOfClass(LivingEntity.class, hurtBB);
        var axis = state.getValue(AXIS);
        for(var e : entities) {
            var pos = getPos();
            var ePos = e.position();
            var direction = new Vec3(
                    axis == Direction.Axis.X ? 0 : ePos.x - pos.getX() - 0.5,
                    axis == Direction.Axis.Y ? 0 : ePos.y - pos.getY() - 0.5,
                    axis == Direction.Axis.Z ? 0 : ePos.z - pos.getZ() - 0.5
            );
            var heading = direction.cross(new Vec3(
                    axis == Direction.Axis.X ? 1 : 0,
                    axis == Direction.Axis.Y ? 1 : 0,
                    axis == Direction.Axis.Z ? 1 : 0
            ));
            var velocity = heading.scale(-getAngularVelocityRadians() * 0.025);
            e.addDeltaMovement(velocity);
            float d = (float) (velocity.length() / 0.16);
            if(!level.isClientSide && d > 1)
                e.hurt(dmgSource, d);
        }
    }

    @Override
    public void writeToSync(FriendlyByteBuf buffer) {
        buffer.writeFloat(getControllerOrThis().getAngularVelocity());
    }

    @Override
    public void readFromSync(FriendlyByteBuf buffer) {
        getControllerOrThis().angularVelocity = buffer.readFloat();
    }

    public interface IForceSource {
        float sourceForce(float velocity);
        float forceSpeed();
        default void receiveUsedForce(float percent) { }
    }
}
