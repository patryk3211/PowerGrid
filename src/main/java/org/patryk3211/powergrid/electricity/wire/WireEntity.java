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

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.network.packets.EntityDataS2CPacket;

import static org.patryk3211.powergrid.electricity.base.ThermalBehaviour.BASE_TEMPERATURE;

public abstract class WireEntity extends Entity implements EntityDataS2CPacket.IConsumer {
    protected static final EntityDataAccessor<Float> TEMPERATURE = SynchedEntityData.defineId(WireEntity.class, EntityDataSerializers.FLOAT);

    // TODO: Transmission line flipping might mess with this. Make sure it is safe.
    private IWireEndpoint endpoint1;
    private IWireEndpoint endpoint2;
    protected byte deferEndpointResolution = 0;
    protected int deferTicks = 0;

    @NotNull
    private WireItem item;
    private int itemCount;

    private ElectricWire wire;
    protected float overheatTemperature = 175f;
    private int despawnTime = 0;
    private int dataVersion = 0;

    private float dissipationFactor;
    private float thermalMass;

    public WireEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public void setWire(ElectricWire wire) {
        this.wire = wire;
    }

    public ElectricWire getWire() {
        return this.wire;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(TEMPERATURE, BASE_TEMPERATURE);
    }

    private void temperatureUpdate() {
        if(!ModdedConfigs.server().electricity.wireOverheating.get()) {
            // No temperature updates when overheating is disabled.
            return;
        }

        if(level().isClientSide)
            return;

        float temperature = entityData.get(TEMPERATURE);
        float energy = 0;
        if (wire != null) {
            // We have to use current here since the wire might be a transmission line,
            // which needs special resistance handling.
            var I = wire.current();
            energy += I * I * getResistance() / 20f;
        }
        if(temperature < overheatTemperature) {
            // If wire is overheated it is considered dead.
            energy -= dissipationFactor * (temperature - BASE_TEMPERATURE) / 20f;
        }
        // This should allow fuses to act
        boolean isSafe = temperature < overheatTemperature - 50f;
        temperature += energy / thermalMass;
        if(temperature >= overheatTemperature && isSafe)
            temperature = overheatTemperature - 25f;

        entityData.set(TEMPERATURE, temperature);
    }

    public boolean isOverheated() {
        return entityData.get(TEMPERATURE) >= overheatTemperature;
    }

    public float getTemperature() {
        return entityData.get(TEMPERATURE);
    }

    @Override
    public void tick() {
        // We don't need Entity#baseTick() in wires
        var world = level();
        temperatureUpdate();
        baseTick();

        if((deferEndpointResolution & 1) != 0) {
            if(endpoint1.isValid(world)) {
                endpoint1.assignWireEntity(this);
                deferEndpointResolution &= ~1;
                makeWire();
            }
        }
        if((deferEndpointResolution & 2) != 0) {
            if(endpoint2.isValid(world)) {
                endpoint2.assignWireEntity(this);
                deferEndpointResolution &= ~2;
                makeWire();
            }
        }
//        if(deferEndpointResolution != 0 && deferTicks++ >= 10) {
//            // If endpoint didn't resolve in 10 ticks it is marked as removed
//            if((deferEndpointResolution & 1) != 0) {
//                endpointRemoved(endpoint1);
//                endpoint1 = null;
//            }
//            if((deferEndpointResolution & 2) != 0) {
//                endpointRemoved(endpoint2);
//                endpoint2 = null;
//            }
//        }

        if(isOverheated()) {
            // Remove to prevent power transfer in the 5 particle ticks.
            dropWire();
            if(!world.isClientSide) {
                if(despawnTime == 0) {
                    ModdedSoundEvents.WIRE_BURNED.playFrom(this);
                }
                if(++despawnTime >= 5) {
                    // Break without dropping items.
                    discard();
                }
            }
        }

        this.firstTick = false;
    }

    public void setEndpoint1(IWireEndpoint endpoint) {
        if(endpoint1 != endpoint) {
            if(endpoint1 != null)
                endpoint1.removeWireEntity(this);
            var world = level();
            if(endpoint != null) {
                if(endpoint.type() == WireEndpointType.DEFERRED_JUNCTION)
                    endpoint = ((DeferredJunctionWireEndpoint) endpoint).resolve(world);
                if(endpoint != null) {
                    if (endpoint.isValid(world)) {
                        endpoint.assignWireEntity(this);
                    } else {
                        deferEndpointResolution |= 1;
                        deferTicks = 0;
                    }
                }
            }
            endpoint1 = endpoint;
            makeWire();
        }
    }

    public void setEndpoint2(IWireEndpoint endpoint) {
        if(endpoint2 != endpoint) {
            if(endpoint2 != null)
                endpoint2.removeWireEntity(this);
            var world = level();
            if(endpoint != null) {
                if(endpoint.type() == WireEndpointType.DEFERRED_JUNCTION)
                    endpoint = ((DeferredJunctionWireEndpoint) endpoint).resolve(world);
                if(endpoint != null) {
                    if (endpoint.isValid(world)) {
                        endpoint.assignWireEntity(this);
                    } else {
                        deferEndpointResolution |= 2;
                        deferTicks = 0;
                    }
                }
            }
            endpoint2 = endpoint;
            makeWire();
        }
    }

    // This method shouldn't be used too much. It's only needed in very special cases.
    public void flipEndpoints() {
        var endpoint = endpoint1;
        endpoint1 = endpoint2;
        endpoint2 = endpoint;
        deferEndpointResolution = (byte) (((deferEndpointResolution & 1) << 1) | ((deferEndpointResolution & 2) >> 1));
        PowerGrid.LOGGER.debug("Wire entity endpoints have been flipped.");
    }

    public IWireEndpoint getEndpoint1() {
        return endpoint1;
    }

    public IWireEndpoint getEndpoint2() {
        return endpoint2;
    }

    public void endpointRemoved(IWireEndpoint endpoint) {

    }

    private EntityDataS2CPacket createExtraDataPacket() {
        var tag = new CompoundTag();
        addAdditionalSaveData(tag);
        tag.putInt("Version", dataVersion++);
        return new EntityDataS2CPacket(this, tag);
    }

    public void sendExtraData() {
        ModdedPackets.sendToClientsTracking(createExtraDataPacket(), this);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        var extra = createExtraDataPacket();
        ModdedPackets.sendToClientsTracking(extra, this);
        return super.getAddEntityPacket();
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
    }

    @Override
    public void onEntityDataPacket(CompoundTag data) {
        int version = data.getInt("Version");
        if(version < dataVersion) {
            // Discard outdated packet.
            return;
        }
        readAdditionalSaveData(data);
        dataVersion = version + 1;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        if(nbt.contains("Item")) {
            var itemTag = nbt.getCompound("Item");
            var readItem = BuiltInRegistries.ITEM.get(new ResourceLocation(itemTag.getString("Id")));
            if(!(readItem instanceof WireItem))
                throw new IllegalStateException("WireEntity item must be a WireItem");
            setItem((WireItem) readItem, itemTag.getInt("Count"));
        } else {
            throw new IllegalStateException("WireEntity must have an item");
        }

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

        entityData.set(TEMPERATURE, nbt.getFloat("Temperature"));
    }

    public void setItem(WireItem item, int count) {
        this.item = item;
        this.itemCount = count;

        int thermalCount = Math.max(itemCount, 1);
        thermalMass = item.getThermalMass() * thermalCount;
        dissipationFactor = item.getDissipationFactor() * thermalCount;
    }

    public float getResistance() {
        return item.getResistance() * Math.max(itemCount, 1);
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return new ItemStack(item, 1);
    }

    public void makeWire() {
        // Client doesn't make a wire, connections are handled differently.
        var world = level();
        if(world.isClientSide)
            return;

        dropWire();

        // Cannot make a wire unless both endpoints are valid.
        if(endpoint1 == null || endpoint2 == null)
            return;

        if(!endpoint1.isValid(world) || !endpoint2.isValid(world))
            return;

        try {
            wire = GlobalElectricNetworks.makeConnection(world, endpoint1, endpoint2, this);
        } catch(RuntimeException e) {
            PowerGrid.LOGGER.error("Failed to create wire for entity", e);
            kill();
        }
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
    protected void addAdditionalSaveData(CompoundTag nbt) {
        if(endpoint1 != null)
            nbt.put("Endpoint1", endpoint1.serialize());

        if(endpoint2 != null)
            nbt.put("Endpoint2", endpoint2.serialize());

        var itemTag = new CompoundTag();
        itemTag.putString("Id", BuiltInRegistries.ITEM.getKey(item).toString());
        itemTag.putInt("Count", itemCount);
        nbt.put("Item", itemTag);

        nbt.putFloat("Temperature", entityData.get(TEMPERATURE));
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if(reason.shouldDestroy()) {
            dropWire();
            if(endpoint1 != null)
                endpoint1.removeWireEntity(this);
            if(endpoint2 != null)
                endpoint2.removeWireEntity(this);
        }
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        var reason = getRemovalReason();
        if(reason.shouldDestroy()) {
            dropWire();
            if(endpoint1 != null)
                endpoint1.removeWireEntity(this);
            if(endpoint2 != null)
                endpoint2.removeWireEntity(this);
        }
    }

    @Override
    public void kill() {
        for(int i = itemCount; i > 0; i -= 64) {
            spawnAtLocation(new ItemStack(item, Math.min(i, 64)));
        }
        itemCount = 0;
        super.kill();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if(player.getItemInHand(hand).getItem() == ModdedItems.WIRE_CUTTER.get()) {
            ModdedSoundEvents.WIRE_CUT.playAt(level(), position(), 0.75f, 1.25f, false);
            kill();
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    public WireItem getWireItem() {
        return item;
    }

    public int getWireCount() {
        return itemCount;
    }

    public void incrementWireCount(int count) {
        itemCount += count;
        if(itemCount < 0)
            itemCount = 0;

        int thermalCount = Math.max(itemCount, 1);
        thermalMass = item.getThermalMass() * thermalCount;
        dissipationFactor = item.getDissipationFactor() * thermalCount;
    }

    @Override
    public void setSharedFlagOnFire(boolean onFire) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    public static void entityUnload(Entity entity, ServerLevel world) {
        if(!(entity instanceof WireEntity wire))
            return;
        if(wire.wire instanceof TransmissionLinePart part) {
            var reason = wire.getRemovalReason();
            if(reason != null && reason.shouldDestroy()) {
                wire.dropWire();
            } else {
                part.unload();
                wire.wire = null;
            }
        } else {
            wire.dropWire();
        }
    }
}
