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
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.network.packets.EntityDataS2CPacket;

import java.util.List;

import static org.patryk3211.powergrid.electricity.base.ThermalBehaviour.BASE_TEMPERATURE;

public abstract class WireEntity extends Entity implements EntityDataS2CPacket.IConsumer {
    // TODO: These have to be taken from the used item.
    public static final float DISSIPATION_FACTOR = 0.2f;
    public static final float THERMAL_MASS = 1f;

    protected static final TrackedData<Float> TEMPERATURE = DataTracker.registerData(WireEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private IWireEndpoint endpoint1;
    private IWireEndpoint endpoint2;

    protected ItemStack item;
    protected float resistance;

    private ElectricWire wire;
    protected float overheatTemperature = 175f;
    private int despawnTime = 0;

    public WireEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void setWire(ElectricWire wire) {
        this.wire = wire;
    }

    public ElectricWire getWire() {
        return this.wire;
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

    public void setEndpoint1(IWireEndpoint endpoint) {
        var world = getWorld();
        var pos = getBlockPos();
        var id = getUuid();
        if(endpoint1 != endpoint) {
            if(endpoint1 != null)
                endpoint1.removeWireEntity(world, id);
            if(endpoint != null)
                endpoint.assignWireEntity(world, pos, id);
            endpoint1 = endpoint;
        }
    }

    public void setEndpoint2(IWireEndpoint endpoint) {
        var world = getWorld();
        var pos = getBlockPos();
        var id = getUuid();
        if(endpoint2 != endpoint) {
            if(endpoint2 != null)
                endpoint2.removeWireEntity(world, id);
            if(endpoint != null)
                endpoint.assignWireEntity(world, pos, id);
            endpoint2 = endpoint;
        }
    }

    public IWireEndpoint getEndpoint1() {
        return endpoint1;
    }

    public IWireEndpoint getEndpoint2() {
        return endpoint2;
    }

    public EntityDataS2CPacket createExtraDataPacket() {
        var extra = new EntityDataS2CPacket(this, 0);
        var tag = new NbtCompound();
        writeCustomDataToNbt(tag);
        extra.buffer.writeNbt(tag);
        return extra;
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        var base = super.createSpawnPacket();
        var extra = createExtraDataPacket();
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
        if(nbt.contains("Endpoint1")) {
            setEndpoint1(WireEndpointType.deserialize(nbt.getCompound("Endpoint1")));
        } else {
            setEndpoint1(null);
        }

        if(nbt.contains("Endpoint2")) {
            setEndpoint2(WireEndpointType.deserialize(nbt.getCompound("Endpoint2")));
        } else {
            setEndpoint2(null);
        }

        if(endpoint1 == null && endpoint2 == null) {
            // At least one endpoint must be defined
            discard();
            return;
        }

        if(nbt.contains("Item"))
            item = ItemStack.fromNbt(nbt.getCompound("Item"));

        dataTracker.set(TEMPERATURE, nbt.getFloat("Temperature"));
        resistance = nbt.getFloat("Resistance");

        if(item == null) {
            // Wires with missing item stack are not allowed.
            discard();
            return;
        }

        makeWire();
    }

    public void makeWire() {
        if(wire != null) {
            wire.remove();
        }

        // Cannot make a wire unless both endpoints are valid.
        if(endpoint1 == null || endpoint2 == null)
            return;

        var world = getWorld();
        wire = GlobalElectricNetworks.makeConnection(world, endpoint1, endpoint2, resistance);
    }

    public void dropWire() {
        if(wire != null) {
            wire.remove();
            wire = null;
        }
    }

    public boolean isConnectedTo(BlockPos pos, int terminal) {
        var testPoint = new BlockWireEndpoint(pos, terminal);
        return testPoint.equals(endpoint1) || testPoint.equals(endpoint2);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if(endpoint1 != null)
            nbt.put("Endpoint1", endpoint1.serialize());

        if(endpoint2 != null)
            nbt.put("Endpoint2", endpoint2.serialize());

        if(item != null)
            nbt.put("Item", item.writeNbt(new NbtCompound()));

        nbt.putFloat("Temperature", dataTracker.get(TEMPERATURE));
        nbt.putFloat("Resistance", resistance);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);

        if(reason.shouldDestroy()) {
            var world = getWorld();

            dropWire();
            if(endpoint1 != null)
                endpoint1.removeWireEntity(world, getUuid());
            if(endpoint2 != null)
                endpoint2.removeWireEntity(world, getUuid());
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
            getWorld().playSoundFromEntity(null, this, ModdedSoundEvents.WIRE_CUT.getMainEvent(), SoundCategory.BLOCKS, 0.75f, 1.25f);
            kill();
            return ActionResult.SUCCESS;
        }
        System.out.printf("Temperature: %f\n", dataTracker.get(TEMPERATURE));
        return super.interact(player, hand);
    }

    public WireItem getWireItem() {
        if(item != null)
            return (WireItem) item.getItem();
        return null;
    }

    public int getWireCount() {
        if(item == null)
            return 0;
        return item.getCount();
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
