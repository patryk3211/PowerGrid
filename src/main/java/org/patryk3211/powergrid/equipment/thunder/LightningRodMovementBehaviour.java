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
package org.patryk3211.powergrid.equipment.thunder;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.entity.EntityType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableObject;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.particles.ZapParticleData;
import org.patryk3211.powergrid.mixin.LightningAccessor;
import org.patryk3211.powergrid.network.packets.LightningSyncS2CPacket;

import java.util.ArrayList;

import static net.minecraft.state.property.Properties.FACING;

public class LightningRodMovementBehaviour implements MovementBehaviour {
    @Override
    public boolean isActive(MovementContext context) {
        return MovementBehaviour.super.isActive(context) && context.contraption instanceof BearingContraption;
    }

    protected void fire(MovementContext context) {
        spawnLightning((ServerWorld) context.world, context.position);
        var lightningEntity = EntityType.LIGHTNING_BOLT.create(context.world);
        if(lightningEntity != null) {
            lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(BlockPos.ofFloored(context.position)));
            lightningEntity.setCosmetic(false);
            ((ServerWorld) context.world).spawnNewEntityAndPassengers(lightningEntity);
        }
        ModdedPackets.sendToClientsTracking(new LightningSyncS2CPacket(context), context.contraption.entity);
    }

    public void pickController(MovementContext context) {
        // Pick the fastest lightning rod as the controller
        var actors = new ArrayList<MovementContext>();
        var fastestActor = new MutableObject<MovementContext>();
        context.contraption.forEachActor(context.world, (behaviour, innerContext) -> {
            if(behaviour instanceof LightningRodMovementBehaviour) {
                actors.add(innerContext);
                if(fastestActor.getValue() == null) {
                    fastestActor.setValue(innerContext);
                } else {
                    var maxSpeed = fastestActor.getValue().motion.length();
                    var thisSpeed = innerContext.motion.length();
                    if(thisSpeed > maxSpeed)
                        fastestActor.setValue(innerContext);
                }
            }
        });
        // Different from the current controller
        if(fastestActor.getValue() != context.temporaryData) {
            actors.forEach(actor -> actor.temporaryData = fastestActor.getValue());
        }
    }

    private static void spawnLightning(ServerWorld world, Vec3d pos) {
        var blockPos = ((LightningAccessor) world).invokeGetLightningPos(BlockPos.ofFloored(pos));
        // This is equivalent to the natural lightning spawning code in ServerWorld
        if(world.hasRain(blockPos)) {
            var lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
            if(lightningEntity != null) {
                lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(blockPos));
                lightningEntity.setCosmetic(false);
                world.spawnNewEntityAndPassengers(lightningEntity);
            }
        }
    }

    public void fireClient(MovementContext context) {
        context.contraption.forEachActor(context.world, (behaviour, innerContext) -> {
            var pos = innerContext.position;
            var facing = innerContext.state.get(FACING);
            SparkParticleData.explodeParticles(innerContext.world, (float) pos.x, (float) pos.y, (float) pos.z, facing, 20);
        });
    }

    @Override
    public void tick(MovementContext context) {
        pickController(context);

        boolean isController = context.temporaryData == context;
        var charge = isController ? context.data.getFloat("Charge") : ((MovementContext) context.temporaryData).data.getFloat("Charge");

        var bearing = (BearingContraption) context.contraption;
        var speed = context.motion.length();
        var sails = bearing.getSailBlocks();

        var configs = ModdedConfigs.server().kinetics;
        var speedFactor = (float) Math.min(speed * configs.lightningAttractorSpeedFactor.getF(), 1.0f);
        var sailFactor = Math.min(sails * configs.lightningAttractorSailFactor.getF(), 1.0f);

        var facing = context.state.get(FACING);
        var facingVec = Vec3d.of(facing.getVector());

        if(isController) {
            if (context.world.isThundering() && speed > 1.5f) {
                charge += speedFactor * sailFactor * configs.lightningAttractorMaxFrequency.getF();
                if (charge >= 1.0f) {
                    charge = 0;
                    if (!context.world.isClient) {
                        fire(context);
                    } else {
                        charge = 1.0f;
                    }
                }
            } else {
                if (charge > 0) {
                    charge -= 0.05f;
                    if (charge < 0)
                        charge = 0;
                }
            }
        }

        if(context.world.isClient) {
            var r = context.world.random;
            var pos1 = context.position;
            var vel1 = facingVec.crossProduct(context.motion);
            // Electric sparks
            var chance = r.nextFloat() * charge * 8f;
            while(r.nextFloat() < chance) {
                var vel = vel1.multiply((r.nextFloat() - 0.5f) * 3.0f)
                        .addRandom(r, 2.0f);
                var pos = pos1.addRandom(r, 2.0f);
                context.world.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
                chance -= 1.0f;
            }
            // Zaps
            chance = r.nextFloat() * charge * 0.1f;
            while(r.nextFloat() < chance) {
                var dir = pos1.addRandom(r, 8.0f);
                context.world.addParticle(new ZapParticleData(dir.x, dir.y, dir.z, false).withLife(0), pos1.x, pos1.y, pos1.z, 0, 0, 0);
                chance -= 1.0f;
            }
        }

        context.data.putFloat("Charge", charge);
    }

    @Override
    public void writeExtraData(MovementContext context) {
        MovementBehaviour.super.writeExtraData(context);
    }
}
