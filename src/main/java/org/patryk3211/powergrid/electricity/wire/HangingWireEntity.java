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

import dev.ryanhcode.sable.companion.SableCompanion;
import net.createmod.ponder.api.level.PonderLevel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.EntityDataS2CPacket;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.IComplexRaycast;
import org.patryk3211.powergrid.utility.SableUtils;

public class HangingWireEntity extends WireEntity implements IComplexRaycast {
    public static final int CLEARANCE_CHECK_INTERVAL = 20;

    private static final Vec3 UP = new Vec3(0, 1, 0);

    public Vec3 terminalPos1;
    public Vec3 terminalPos2;
    public AABB deSabledBB;
    private boolean isDynamic = false;
    private Vec3 baseTerminalPos1;
    private Vec3 baseTerminalPos2;
    Vec3 terminal1Velocity;
    Vec3 terminal2Velocity;
    private float placedLength;

    private boolean particlesSpawned = false;

    public CurveParameters curveParams;

    private int clearanceCheck = 0;

    public HangingWireEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public static boolean checkClearance(Level world, Vec3 start, Vec3 end) {
        if(!SableUtils.sameSubLevel(world, start, end))
            return true;
        var result = BlockTrace.raycast(world, start, end);
        if(result.getType() == HitResult.Type.BLOCK) {
            if(world.isClientSide) {
                WirePreview.notifyOfBlock(result.getBlockPos());
            }
            return false;
        }
        return true;
    }

    public void updateRenderParams() {
        var item = getWireEntry();
        curveParams = new CurveParameters(terminalPos1, terminalPos2, placedLength, item.wireThickness());
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Nullable
    @Environment(EnvType.CLIENT)
    public AABB calculateClientBoundingBox() {
        if(curveParams == null)
            return null;
        var pos = position();
        var box = new AABB(terminalPos1, terminalPos2);
        var minY = new MutableFloat(box.minY);
        final float eY = (float) pos.y;
        curveParams.runForSegments((x1, y1, z1, x2, y2, z2, offset, length) -> {
            double y = (y1 + y2) * 0.5 + eY;
            if(y < minY.getValue())
                minY.setValue(y);
        }, 0.5f);
        deSabledBB = new AABB(
                terminalPos1.x - pos.x, terminalPos1.y - pos.y, terminalPos1.z - pos.z,
                terminalPos2.x - pos.x, terminalPos2.y - pos.y, terminalPos2.z - pos.z
        );
        deSabledBB = deSabledBB.setMinY(deSabledBB.minY + minY.getValue() - box.minY).inflate(0.1f);
        return box.setMinY(minY.getValue()).inflate(0.1f);
    }

    @Override
    public AABB getDeSabledBB() {
        if(SableCompanion.INSTANCE.getContaining(this) == null || deSabledBB == null)
            return getBoundingBox();
        return deSabledBB.move(SableCompanion.INSTANCE.projectOutOfSubLevel(level(), position()));
    }

    public static HangingWireEntity create(Level world, BlockWireEndpoint endpoint1, BlockWireEndpoint endpoint2, ItemStack item, @Nullable Float resistance) {
        if(!IWire.isWire(world, item.getItem()))
            throw new IllegalArgumentException("ItemStack must be of a WireItem");
        var entity = new HangingWireEntity(ModdedEntities.HANGING_WIRE.get(), world);
        entity.setItem(item.getItem(), item.getCount());

        entity.resistanceOverride = resistance;

        entity.setEndpoint1(endpoint1);
        entity.setEndpoint2(endpoint2);

        var tp1 = SableCompanion.INSTANCE.projectOutOfSubLevel(world, endpoint1.getExactPosition(world));
        var tp2 = SableCompanion.INSTANCE.projectOutOfSubLevel(world, endpoint2.getExactPosition(world));
        var dX = tp2.x - tp1.x;
        var dY = tp2.y - tp1.y;
        var dZ = tp2.z - tp1.z;
        var hL = dX * dX + dZ * dZ;
        entity.placedLength = (float) Math.sqrt(entity.getWireEntry().horizontalCoefficient() * hL + entity.getWireEntry().verticalCoefficient() * dY * dY);

        entity.refreshTerminalPositions();
        entity.setXRot(0);
        entity.setOldPosAndRot();
        entity.reapplyPosition();

        if(entity.getWireEntry().colorable())
            entity.setColor(0x413c31);

        return entity;
    }

    @Override
    protected AABB makeBoundingBox() {
        if(terminalPos1 != null && terminalPos2 != null) {
            if(level().isClientSide) {
                var box = calculateClientBoundingBox();
                if(box != null)
                    return box;
            }
            var box = new AABB(terminalPos1, terminalPos2);
            return box.inflate(0.1f);
        } else
            return super.makeBoundingBox();
    }

    @Override
    public void tick() {
        var beginFlags = deferEndpointResolution;
        super.tick();
        var world = level();
        if(beginFlags != deferEndpointResolution) {
            grabEndpointPositions();
            updateRenderParams();
        }
        if(isDynamic && baseTerminalPos1 != null && baseTerminalPos2 != null) {
            terminalPos1 = SableCompanion.INSTANCE.projectOutOfSubLevel(world, baseTerminalPos1);
            terminalPos2 = SableCompanion.INSTANCE.projectOutOfSubLevel(world, baseTerminalPos2);
            terminal1Velocity = SableCompanion.INSTANCE.getVelocity(world, baseTerminalPos1);
            terminal2Velocity = SableCompanion.INSTANCE.getVelocity(world, baseTerminalPos2);
            var vect = terminalPos2.subtract(terminalPos1);
            var facing = vect.cross(UP);
            float facingAngle = (float) (Math.atan2(facing.x, -facing.z) * 180 / Math.PI);

            setOldPosAndRot();
            setPosRaw(
                    (terminalPos1.x + terminalPos2.x) * 0.5,
                    terminalPos1.y,
                    (terminalPos1.z + terminalPos2.z) * 0.5
            );
            setYRot(facingAngle);
            updateRenderParams();
        }
        if(curveParams != null && !curveParams.valid) {
            kill();
            return;
        }

        var temperature = getTemperature();

        var pos = position();
        if(isOverheated()) {
            if(world.isClientSide && !particlesSpawned) {
                float dx = (float) curveParams.getCurveSpan();
                int pointCount = Math.round(dx / 0.25f);
                curveParams.runForPoints(pointCount, (x, y, z) -> {
                    world.addParticle(ParticleTypes.FLAME,
                            pos.x + x, pos.y + y, pos.z + z,
                            0.0f, 0.00f, 0.0f);
                });
                particlesSpawned = true;
            }
        } else if(temperature >= overheatTemperature - 50 && world.isClientSide && curveParams != null) {
            var curvePoint = curveParams.getRandomPoint(random);
            double x = curvePoint.x + pos.x;
            double y = curvePoint.y + pos.y;
            double z = curvePoint.z + pos.z;
            world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
        }

        if(!isDynamic && !world.isClientSide && clearanceCheck++ >= CLEARANCE_CHECK_INTERVAL && terminalPos1 != null && terminalPos2 != null) {
            clearanceCheck = 0;
            var result = BlockTrace.raycast(world, terminalPos1, terminalPos2);
            if(result.getType() == HitResult.Type.BLOCK) {
                if(getEndpoint1() instanceof BlockWireEndpoint block) {
                    if(block.getPos().equals(result.getBlockPos()))
                        return;
                } else if(getEndpoint2() instanceof BlockWireEndpoint block) {
                    if(block.getPos().equals(result.getBlockPos()))
                        return;
                }
                kill();
            }
        }
    }

    @Override
    public void endpointRemoved(IWireEndpoint endpoint) {
        if(endpoint.equals(getEndpoint1()) || endpoint.equals(getEndpoint2()))
            kill();
    }

    @Override
    public boolean isPickable() {
        // Hits get handled by IComplexRaycast
        return curveParams != null && curveParams.isVertical();
    }

    @Override
    public void onEntityDataPacket(CompoundTag data) {
        if(data.contains("V")) {
            var list = data.getList("V", Tag.TAG_FLOAT);
            terminalPos1 = new Vec3(list.getFloat(0), list.getFloat(1), list.getFloat(2));
            terminalPos2 = new Vec3(list.getFloat(3), list.getFloat(4), list.getFloat(5));
            isDynamic = data.getBoolean("D");
            updateRenderParams();
        } else {
            super.onEntityDataPacket(data);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("PlacedLength", placedLength);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if(getEndpoint1() == null || getEndpoint2() == null) {
            kill();
            return;
        }

        var world = level();
        if(!world.isClientSide) {
            refreshTerminalPositions();
        } else {
            grabEndpointPositions();
        }

        if(nbt.contains("PlacedLength")) {
            placedLength = nbt.getFloat("PlacedLength");
        } else {
            var dX = terminalPos2.x - terminalPos1.x;
            var dY = terminalPos2.y - terminalPos1.y;
            var dZ = terminalPos2.z - terminalPos1.z;
            var hL = dX * dX + dZ * dZ;
            placedLength = (float) Math.sqrt(getWireEntry().horizontalCoefficient() * hL + getWireEntry().verticalCoefficient() * dY * dY);
        }
        updateRenderParams();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public @Nullable Vec3 raycast(Vec3 min, Vec3 max) {
        // TODO: Sometimes this raycast is really finicky
        if(getWireEntry() == null)
            return null;
        var thickness = getWireEntry().wireThickness() * 2;
        if(curveParams != null) {
            Vec3 ray = max.subtract(min);
            var rayLength = ray.lengthSqr();
            ray = ray.normalize();
            Vec3 planeOrigin = SableCompanion.INSTANCE.projectOutOfSubLevel(level(), position());
            Vec3 planeNormal = getViewVector(1);
            Vec3 planeOriginVector = planeOrigin.subtract(min);

            var planeYVector = new Vec3(0, 1, 0);
            var planeXVector = planeNormal.cross(planeYVector);
            if(ray.x * ray.x + ray.z * ray.z < ray.y * ray.y * 0.25) {
                double planeDistance = planeOriginVector.dot(planeYVector);

                double hitDistance = planeDistance / planeYVector.dot(ray);
                if(hitDistance * hitDistance < rayLength) {
                    Vec3 hit = min.add(ray.scale(hitDistance));

                    var hitOriginVector = hit.subtract(planeOrigin);

                    var parallelDistance = Math.abs(planeXVector.dot(hitOriginVector));
                    var perpendicularDistance = Math.abs(planeNormal.dot(hitOriginVector));

                    if(parallelDistance < curveParams.getCurveSpan() / 2 && perpendicularDistance < thickness / 2) {
                        // Hit
                        return hit;
                    } else {
                        // Miss
                        return null;
                    }
                }
            } else {
                double planeDistance = planeOriginVector.dot(planeNormal);

                double hitDistance = planeDistance / planeNormal.dot(ray);
                if (hitDistance > 0 && hitDistance * hitDistance < rayLength) {
                    Vec3 hit = min.add(ray.scale(hitDistance));

                    var hitOriginVector = hit.subtract(planeOrigin);
                    double x = planeXVector.dot(hitOriginVector);
                    // We can do that since the entity never has any pitch.
                    double y = hitOriginVector.y;

                    double closeX = curveParams.findClosestPoint(x, y);
                    double span = curveParams.getCurveSpan() / 2;
                    closeX = Math.min(Math.max(closeX, -span), span);

                    double dX = x - closeX;
                    double dY = y - curveParams.apply((float) closeX);

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
    public void flipEndpoints() {
        super.flipEndpoints();
        refreshTerminalPositions();
    }

    public void grabEndpointPositions() {
        var world = level();
        terminalPos1 = getEndpoint1().getExactPosition(world);
        terminalPos2 = getEndpoint2().getExactPosition(world);
        if(getEndpoint1().getSubLevel(world) != null || getEndpoint2().getSubLevel(world) != null) {
            // Make outside of sublevels
            baseTerminalPos1 = terminalPos1;
            baseTerminalPos2 = terminalPos2;
            isDynamic = true;
            terminalPos1 = SableCompanion.INSTANCE.projectOutOfSubLevel(world, terminalPos1);
            terminalPos2 = SableCompanion.INSTANCE.projectOutOfSubLevel(world, terminalPos2);
        } else {
            baseTerminalPos1 = null;
            baseTerminalPos2 = null;
            isDynamic = false;
        }
    }

    public void refreshTerminalPositions() {
        var world = level();
        grabEndpointPositions();
        if(world != null && (!world.isClientSide || world instanceof PonderLevel)) {
            var vect = terminalPos2.subtract(terminalPos1);
            var facing = vect.cross(UP);
            float facingAngle = (float) (Math.atan2(facing.x, -facing.z) * 180 / Math.PI);

            setPos(
                    (terminalPos1.x + terminalPos2.x) * 0.5,
                    terminalPos1.y,
                    (terminalPos1.z + terminalPos2.z) * 0.5
            );
            setYRot(facingAngle);

            updateRenderParams();
            if(!world.isClientSide) {
                // I guess we have to do position update like that because otherwise,
                // the update method would have to go into the tick function
                // and that is probably slower.

                var tag = new CompoundTag();
                var list = new ListTag();
                list.add(FloatTag.valueOf((float) terminalPos1.x));
                list.add(FloatTag.valueOf((float) terminalPos1.y));
                list.add(FloatTag.valueOf((float) terminalPos1.z));
                list.add(FloatTag.valueOf((float) terminalPos2.x));
                list.add(FloatTag.valueOf((float) terminalPos2.y));
                list.add(FloatTag.valueOf((float) terminalPos2.z));
                tag.putBoolean("D", isDynamic);
                tag.put("V", list);
                var packet = new EntityDataS2CPacket(this, tag);
                ModdedPackets.sendToClientsTracking(packet, this);
            }
        }
    }
}
