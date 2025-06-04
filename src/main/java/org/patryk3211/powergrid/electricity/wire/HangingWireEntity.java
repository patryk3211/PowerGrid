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
package org.patryk3211.powergrid.electricity.wire;

import com.simibubi.create.foundation.ponder.PonderWorld;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.network.packets.EntityDataS2CPacket;
import org.patryk3211.powergrid.utility.IComplexRaycast;

public class HangingWireEntity extends WireEntity implements IComplexRaycast {
    private static final Vec3d UP = new Vec3d(0, 1, 0);

    public Vec3d terminalPos1;
    public Vec3d terminalPos2;

    private boolean particlesSpawned = false;

    public Object renderParams;

    public HangingWireEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void updateRenderParams() {
        if(!getWorld().isClient)
            return;
        this.setBoundingBox(this.calculateBoundingBox());
        var item = getWireItem();
        renderParams = new CurveParameters(terminalPos1, terminalPos2,
                item.getHorizontalCoefficient(), item.getVerticalCoefficient(), item.getWireThickness());
    }

    public static HangingWireEntity create(World world, BlockWireEndpoint endpoint1, BlockWireEndpoint endpoint2, ItemStack item, float resistance) {
        var entity = new HangingWireEntity(ModdedEntities.HANGING_WIRE.get(), world);
        entity.item = item;
        entity.resistance = resistance;

        entity.setEndpoint1(endpoint1);
        entity.setEndpoint2(endpoint2);

        entity.refreshTerminalPositions();
        entity.setPitch(0);
        entity.resetPosition();
        entity.refreshPosition();

        entity.makeWire();
        return entity;
    }

    @Override
    protected Box calculateBoundingBox() {
        if(terminalPos1 != null && terminalPos2 != null) {
            var box = new Box(terminalPos1, terminalPos2);
            return box.expand(0.1f);
        } else
            return super.calculateBoundingBox();
    }

    @Override
    public void tick() {
        super.tick();
        var world = getWorld();
        var temperature = getTemperature();

        var pos = getPos();
        if(isOverheated()) {
            if(world.isClient && !particlesSpawned) {
                var curveParams = (CurveParameters) renderParams;
                var dx = curveParams.getCurveSpan();
                var normal = curveParams.getNormal();
                int pointCount = Math.round(dx / 0.25f);
                for(int i = 0; i < pointCount; ++i) {
                    float localX = ((float) i / pointCount - 0.5f) * dx;
                    var x = pos.x + localX * normal.x;
                    var y = pos.y + curveParams.apply(localX);
                    var z = pos.z + localX * normal.z;
                    world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0f, 0.00f, 0.0f);
                }
                particlesSpawned = true;
            }
        } else if(temperature >= overheatTemperature - 50 && world.isClient && renderParams != null) {
            var curvePoint = ((CurveParameters) renderParams).getRandomPoint(random);
            double x = curvePoint.x + pos.x;
            double y = curvePoint.y + pos.y;
            double z = curvePoint.z + pos.z;
            world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
        }
    }

    @Override
    public boolean canHit() {
        // Hits get handled by IComplexRaycast
        return false;
    }

    @Override
    public void onEntityDataPacket(EntityDataS2CPacket packet) {
        if(packet.type == 1) {
            var buffer = packet.buffer;
            terminalPos1 = new Vec3d(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
            terminalPos2 = new Vec3d(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
            updateRenderParams();
        } else {
            super.onEntityDataPacket(packet);
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        var world = getWorld();
        if(!world.isClient) {
            refreshTerminalPositions();
        } else {
            terminalPos1 = getEndpoint1().getExactPosition(world);
            terminalPos2 = getEndpoint2().getExactPosition(world);
            updateRenderParams();
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public @Nullable Vec3d raycast(Vec3d min, Vec3d max) {
        // TODO: Sometimes this raycast is really finicky
        var thickness = getWireItem().getWireThickness();
        if(renderParams instanceof CurveParameters params) {
            Vec3d ray = max.subtract(min);
            var rayLength = ray.lengthSquared();
            ray = ray.normalize();
            Vec3d planeOrigin = getPos();
            Vec3d planeNormal = getRotationVec(1);
            Vec3d planeOriginVector = planeOrigin.subtract(min);

            var planeYVector = new Vec3d(0, 1, 0);
            var planeXVector = planeNormal.crossProduct(planeYVector);
            if(ray.x * ray.x + ray.z * ray.z < ray.y * ray.y * 0.25) {
                double planeDistance = planeOriginVector.dotProduct(planeYVector);

                double hitDistance = planeDistance / planeYVector.dotProduct(ray);
                if(hitDistance * hitDistance < rayLength) {
                    Vec3d hit = min.add(ray.multiply(hitDistance));

                    var hitOriginVector = hit.subtract(planeOrigin);

                    var parallelDistance = Math.abs(planeXVector.dotProduct(hitOriginVector));
                    var perpendicularDistance = Math.abs(planeNormal.dotProduct(hitOriginVector));

                    if(parallelDistance < params.getCurveSpan() / 2 && perpendicularDistance < thickness / 2) {
                        // Hit
                        return hit;
                    } else {
                        // Miss
                        return null;
                    }
                }
            } else {
                double planeDistance = planeOriginVector.dotProduct(planeNormal);

                double hitDistance = planeDistance / planeNormal.dotProduct(ray);
                if (hitDistance > 0 && hitDistance * hitDistance < rayLength) {
                    Vec3d hit = min.add(ray.multiply(hitDistance));

                    var hitOriginVector = hit.subtract(planeOrigin);
                    double x = planeXVector.dotProduct(hitOriginVector);
                    // We can do that since the entity never has any pitch.
                    double y = hitOriginVector.y;

                    double closeX = params.findClosestPoint(x, y);
                    double span = params.getCurveSpan() / 2;
                    closeX = Math.min(Math.max(closeX, -span), span);

                    double dX = x - closeX;
                    double dY = y - params.apply((float) closeX);

                    double squareDistance = dX * dX + dY * dY;
                    if(squareDistance < thickness * thickness) {
                        // Hit
                        return hit;
                    } else {
                        // Miss
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void setPosition(double x, double y, double z) {
        super.setPosition(x, y, z);
        // Endpoints need to be refreshed with new entity block pos.
        var e1 = getEndpoint1();
        setEndpoint1(null);
        setEndpoint1(e1);
        var e2 = getEndpoint2();
        setEndpoint2(null);
        setEndpoint2(e2);
    }

    public void refreshTerminalPositions() {
        var world = getWorld();
        if(world != null && (!world.isClient || world instanceof PonderWorld)) {
            terminalPos1 = getEndpoint1().getExactPosition(world);
            terminalPos2 = getEndpoint2().getExactPosition(world);

            var vect = terminalPos2.subtract(terminalPos1);
            var facing = vect.crossProduct(UP);
            float facingAngle = (float) (Math.atan2(facing.x, -facing.z) * 180 / Math.PI);

            setPosition(
                    (terminalPos1.x + terminalPos2.x) * 0.5,
                    terminalPos1.y,
                    (terminalPos1.z + terminalPos2.z) * 0.5
            );
            setYaw(facingAngle);

            if(!world.isClient) {
                // I guess we have to do position update like that because otherwise,
                // the update method would have to go into the tick function
                // and that is probably slower.
                var packet = new EntityDataS2CPacket(this, 1);
                var buffer = packet.buffer;
                buffer.writeFloat((float) terminalPos1.x);
                buffer.writeFloat((float) terminalPos1.y);
                buffer.writeFloat((float) terminalPos1.z);
                buffer.writeFloat((float) terminalPos2.x);
                buffer.writeFloat((float) terminalPos2.y);
                buffer.writeFloat((float) terminalPos2.z);
                packet.send();
            }

            // TODO: There should be some line-of-sight check here to forbid wires going through blocks (it should also be in the entity's tick function).
        }
    }
}
