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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
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
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.network.EntityDataPacket;

import java.util.List;

public class WireEntity extends Entity implements EntityDataPacket.IConsumer {
    private static final Vec3d UP = new Vec3d(0, 1, 0);

    private BlockPos electricBlockPos1;
    private BlockPos electricBlockPos2;

    private int electricTerminal1;
    private int electricTerminal2;

    public Vec3d terminalPos1;
    public Vec3d terminalPos2;

    public Object renderParams;

    public WireEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void updateRenderParams() {
        if(!getWorld().isClient)
            return;
        renderParams = new WireRenderer.RenderParameters(terminalPos1, terminalPos2, 1.01, 1.2, 0.1, getPos());
    }

    public static WireEntity create(ServerWorld world, BlockPos pos1, int terminal1, BlockPos pos2, int terminal2) {
        var entity = new WireEntity(ModdedEntities.WIRE.get(), world);
        entity.electricBlockPos1 = pos1;
        entity.electricBlockPos2 = pos2;
        entity.electricTerminal1 = terminal1;
        entity.electricTerminal2 = terminal2;
        entity.terminalPos1 = IElectric.getTerminalPos(pos1, world.getBlockState(pos1), terminal1);
        entity.terminalPos2 = IElectric.getTerminalPos(pos2, world.getBlockState(pos2), terminal2);

        var vect = entity.terminalPos2.subtract(entity.terminalPos1);
        var facing = vect.crossProduct(UP);
        // TODO: I don't think we need to calculate this angle.
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
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
//        return super.interact(player, hand);
        return ActionResult.SUCCESS;
    }
}
