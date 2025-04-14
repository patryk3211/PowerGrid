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

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.network.EntityDataPacket;
import org.patryk3211.powergrid.utility.IComplexRaycast;

import java.util.List;

public class WireEntity extends Entity implements EntityDataPacket.IConsumer, IComplexRaycast {
    private static final Vec3d UP = new Vec3d(0, 1, 0);

    private static final float THICKNESS = 0.1f;

    private BlockPos electricBlockPos1;
    private BlockPos electricBlockPos2;

    private int electricTerminal1;
    private int electricTerminal2;

    public Vec3d terminalPos1;
    public Vec3d terminalPos2;

    private ItemStack item;

    public Object renderParams;

    public WireEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void updateRenderParams() {
        if(!getWorld().isClient)
            return;
        renderParams = new WireRenderer.CurveParameters(terminalPos1, terminalPos2, 1.01, 1.2, THICKNESS, getPos());
    }

    public static WireEntity create(ServerWorld world, BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, ItemStack item) {
        var entity = new WireEntity(ModdedEntities.WIRE.get(), world);
        entity.electricBlockPos1 = pos1;
        entity.electricBlockPos2 = pos2;
        entity.electricTerminal1 = terminal1;
        entity.electricTerminal2 = terminal2;
        entity.terminalPos1 = IElectric.getTerminalPos(pos1, world.getBlockState(pos1), terminal1);
        entity.terminalPos2 = IElectric.getTerminalPos(pos2, world.getBlockState(pos2), terminal2);
        entity.item = item;

        var vect = entity.terminalPos2.subtract(entity.terminalPos1);
        var facing = vect.crossProduct(UP);
        float facingAngle = (float) (Math.atan2(facing.x, -facing.z) * 180 / Math.PI);

        entity.refreshPositionAndAngles(
                (entity.terminalPos1.x + entity.terminalPos2.x) * 0.5,
                (entity.terminalPos1.y + entity.terminalPos2.y) * 0.5,
                (entity.terminalPos1.z + entity.terminalPos2.z) * 0.5,
               facingAngle, 0);
        entity.updateRenderParams();
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
    protected void initDataTracker() {

    }

    @Override
    public boolean canHit() {
        // Hits get handled by IComplexRaycast
        return false;
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        var base = super.createSpawnPacket();
        var extra = new EntityDataPacket(this);
        var tag = new NbtCompound();
        writeCustomDataToNbt(tag);
        extra.buffer.writeNbt(tag);
        return new BundleS2CPacket(List.of(base, extra.packet()));
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
    }

    @Override
    public void onEntityDataPacket(EntityDataPacket packet) {
        var tag = packet.buffer.readNbt();
        if(tag != null)
            readCustomDataFromNbt(tag);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        var posArr1 = nbt.getIntArray("pos1");
        electricBlockPos1 = new BlockPos(posArr1[0], posArr1[1], posArr1[2]);
        var posArr2 = nbt.getIntArray("pos2");
        electricBlockPos2 = new BlockPos(posArr2[0], posArr2[1], posArr2[2]);

        electricTerminal1 = nbt.getInt("terminal1");
        electricTerminal2 = nbt.getInt("terminal2");

        if(nbt.contains("item"))
            item = ItemStack.fromNbt(nbt.getCompound("item"));

        var world = getWorld();
        if(world != null) {
            terminalPos1 = IElectric.getTerminalPos(electricBlockPos1, world.getBlockState(electricBlockPos1), electricTerminal1);
            terminalPos2 = IElectric.getTerminalPos(electricBlockPos2, world.getBlockState(electricBlockPos2), electricTerminal2);
            updateRenderParams();
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putIntArray("pos1", new int[] { electricBlockPos1.getX(), electricBlockPos1.getY(), electricBlockPos1.getZ() });
        nbt.putIntArray("pos2", new int[] { electricBlockPos2.getX(), electricBlockPos2.getY(), electricBlockPos2.getZ() });
        nbt.putInt("terminal1", electricTerminal1);
        nbt.putInt("terminal2", electricTerminal2);
        if(item != null)
            nbt.put("item", item.writeNbt(new NbtCompound()));
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);

        if(reason.shouldDestroy()) {
            var world = getWorld();

            if(world.getBlockEntity(electricBlockPos1) instanceof SmartBlockEntity smartEntity) {
                var behaviour = smartEntity.getBehaviour(ElectricBehaviour.TYPE);
                behaviour.removeConnection(electricTerminal1, electricBlockPos2, electricTerminal2);
            }

            // This isn't really needed but just to be safe we try to remove from both sides.
            if(world.getBlockEntity(electricBlockPos2) instanceof SmartBlockEntity smartEntity) {
                var behaviour = smartEntity.getBehaviour(ElectricBehaviour.TYPE);
                behaviour.removeConnection(electricTerminal2, electricBlockPos1, electricTerminal1);
            }
        }
    }

    @Override
    public void kill() {
        if(item != null) {
            dropStack(item);
            item = null;
        }
        super.kill();
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if(player.getStackInHand(hand).getItem() == ModdedItems.WIRE_CUTTER.get()) {
            kill();
            return ActionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    @Override
    public void setOnFire(boolean onFire) {
    }

    @Override
    @Environment(EnvType.CLIENT)
    public @Nullable Vec3d raycast(Vec3d min, Vec3d max) {
        if(renderParams instanceof WireRenderer.CurveParameters params) {
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

                    if(parallelDistance < params.getCurveSpan() / 2 && perpendicularDistance < THICKNESS / 2) {
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
                    if(squareDistance < THICKNESS * THICKNESS) {
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
}
