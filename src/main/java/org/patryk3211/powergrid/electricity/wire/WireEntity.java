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
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.network.packets.EntityDataS2CPacket;

import java.util.List;

import static org.patryk3211.powergrid.electricity.base.ThermalBehaviour.BASE_TEMPERATURE;

public abstract class WireEntity extends Entity implements EntityDataS2CPacket.IConsumer {
    // TODO: These have to be taken from the used item.
    public static final float DISSIPATION_FACTOR = 0.2f;
    public static final float THERMAL_MASS = 1f;

    protected static final TrackedData<Float> TEMPERATURE = DataTracker.registerData(WireEntity.class, TrackedDataHandlerRegistry.FLOAT);

    protected BlockPos electricBlockPos1;
    protected BlockPos electricBlockPos2;

    protected int electricTerminal1;
    protected int electricTerminal2;

    protected ItemStack item;

    private ElectricWire wire;
    protected float overheatTemperature = 175f;
    private int despawnTime = 0;

    public WireEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void setWire(ElectricWire wire) {
        this.wire = wire;
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(TEMPERATURE, BASE_TEMPERATURE);
    }

    private void temperatureUpdate() {
        var temperature = dataTracker.get(TEMPERATURE);
        if(getWorld().isClient)
            return;

        float energy = 0;
        if (wire != null) {
            var voltage = wire.potentialDifference();
            energy += voltage * voltage / wire.getResistance() / 20f;
        }
        if(temperature < overheatTemperature) {
            // If wire is overheated it is considered dead.
            energy -= DISSIPATION_FACTOR * (temperature - BASE_TEMPERATURE) / 20f;
        }
        temperature += energy / THERMAL_MASS;
        dataTracker.set(TEMPERATURE, temperature);

    }

    public boolean isOverheated() {
        return dataTracker.get(TEMPERATURE) >= overheatTemperature;
    }

    public float getTemperature() {
        return dataTracker.get(TEMPERATURE);
    }

    @Override
    public void tick() {
        super.tick();
        var world = getWorld();
        temperatureUpdate();

        if(isOverheated()) {
            if(wire != null) {
                // Remove to prevent power transfer in the 5 particle ticks.
                wire.remove();
                wire = null;
            }
            if(!world.isClient) {
                if(++despawnTime >= 5) {
                    // Break without dropping items.
                    discard();
                }
            }
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        var base = super.createSpawnPacket();
        var extra = new EntityDataS2CPacket(this, 0);
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
    public void onEntityDataPacket(EntityDataS2CPacket packet) {
        if(packet.type == 0) {
            var tag = packet.buffer.readNbt();
            if (tag != null)
                readCustomDataFromNbt(tag);
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        var posArr1 = nbt.getIntArray("Pos1");
        electricBlockPos1 = new BlockPos(posArr1[0], posArr1[1], posArr1[2]);
        var posArr2 = nbt.getIntArray("Pos2");
        electricBlockPos2 = new BlockPos(posArr2[0], posArr2[1], posArr2[2]);

        electricTerminal1 = nbt.getInt("Terminal1");
        electricTerminal2 = nbt.getInt("Terminal2");

        if(nbt.contains("Item"))
            item = ItemStack.fromNbt(nbt.getCompound("Item"));

        dataTracker.set(TEMPERATURE, nbt.getFloat("Temperature"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putIntArray("Pos1", new int[] { electricBlockPos1.getX(), electricBlockPos1.getY(), electricBlockPos1.getZ() });
        nbt.putIntArray("Pos2", new int[] { electricBlockPos2.getX(), electricBlockPos2.getY(), electricBlockPos2.getZ() });
        nbt.putInt("Terminal1", electricTerminal1);
        nbt.putInt("Terminal2", electricTerminal2);
        if(item != null)
            nbt.put("Item", item.writeNbt(new NbtCompound()));

        nbt.putFloat("Temperature", dataTracker.get(TEMPERATURE));
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
        System.out.printf("Temperature: %f\n", dataTracker.get(TEMPERATURE));
        return super.interact(player, hand);
    }

    @Override
    public void setOnFire(boolean onFire) {
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return super.damage(source, amount);
    }

    @Override
    public PistonBehavior getPistonBehavior() {
        return PistonBehavior.IGNORE;
    }
}
