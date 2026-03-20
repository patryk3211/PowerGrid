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

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.*;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.DebugItem;
import org.patryk3211.powergrid.equipment.multimeter.MultimeterItem;
import org.patryk3211.powergrid.network.packets.EntityDataS2CPacket;

import static org.patryk3211.powergrid.electricity.base.ThermalBehaviour.BASE_TEMPERATURE;

public abstract class BaseWireEntity extends Entity implements EntityDataS2CPacket.IConsumer {
    protected static final EntityDataAccessor<Float> TEMPERATURE = SynchedEntityData.defineId(BaseWireEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Byte> OVERHEAT_TICKS = SynchedEntityData.defineId(BaseWireEntity.class, EntityDataSerializers.BYTE);

    protected IWireEndpoint endpoint1;
    protected IWireEndpoint endpoint2;
    protected byte deferEndpointResolution = 0;
    protected int deferTicks = 0;
    private boolean overheated = false;

    @NotNull
    private WireItem item;
    protected int itemCount;
    private int color;

    protected float overheatTemperature = 175f;
    private int despawnTime = 0;
    private int dataVersion = 0;

    private float dissipationFactor;
    private float thermalMass;

    protected Float resistanceOverride = null;

    public BaseWireEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public abstract float current();
    public abstract float measuredCurrent();

    @Override
    protected void defineSynchedData() {
        entityData.define(TEMPERATURE, BASE_TEMPERATURE);
        entityData.define(OVERHEAT_TICKS, (byte) 0);
    }

    protected boolean testForOverheat(float temperature, float energy) {
        return temperature > overheatTemperature && energy > 0;
    }

    protected boolean testForCooling(float energy) {
        return energy <= 0;
    }

    private void temperatureUpdate() {
        if(!ModdedConfigs.server().electricity.wireOverheating.get()) {
            // No temperature updates when overheating is disabled.
            return;
        }

        float temperature = entityData.get(TEMPERATURE);
        overheated = entityData.get(OVERHEAT_TICKS) >= ThermalBehaviour.OVERHEAT_TICKS;
        if(level().isClientSide && !(level() instanceof PonderLevel))
            return;

        float energy = 0;
        // We have to use current here since the wire might be a transmission line,
        // which needs special resistance handling.
        var I = current();
        energy += I * I * getResistance() / 20f;
        if(!overheated) {
            // If wire is overheated it is considered dead.
            energy -= dissipationFactor * (temperature - BASE_TEMPERATURE) / 20f;
            temperature += energy / thermalMass;

            if(testForOverheat(temperature, energy)) {
                // Temperature is high and keeps rising.
                entityData.set(OVERHEAT_TICKS, (byte) (entityData.get(OVERHEAT_TICKS) + 1));
            } else if(testForCooling(energy)) {
                // Cooling down so it's fine.
                entityData.set(OVERHEAT_TICKS, (byte) 0);
                if(temperature > overheatTemperature + 10)
                    temperature = overheatTemperature + 10;
            }
        }

        entityData.set(TEMPERATURE, temperature);
    }

    public boolean isOverheated() {
        return overheated;
    }

    public float getTemperature() {
        return entityData.get(TEMPERATURE);
    }

    @Override
    public void tick() {
        // We don't need Entity#baseTick() in wires
        var world = level();
        temperatureUpdate();

        if((deferEndpointResolution & 1) != 0) {
            if(endpoint1 != null && endpoint1.isValid(world)) {
                endpoint1.assignWireEntity(this);
                deferEndpointResolution &= ~1;
                makeWire();
            }
        }
        if((deferEndpointResolution & 2) != 0) {
            if(endpoint2 != null && endpoint2.isValid(world)) {
                endpoint2.assignWireEntity(this);
                deferEndpointResolution &= ~2;
                makeWire();
            }
        }

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
        assert endpoint == null || endpoint.canAcceptType(this.getClass()) : "Endpoint doesn't accept this entity type";
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
        assert endpoint == null || endpoint.canAcceptType(this.getClass()) : "Endpoint doesn't accept this entity type";
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
            if(!(readItem instanceof WireItem wireItem))
                throw new IllegalStateException("WireEntity item must be a WireItem");
            setItem(wireItem, itemTag.getInt("Count"));
            if(wireItem.canBeColored())
                color = nbt.getInt("Color");
        } else {
            throw new IllegalStateException("WireEntity must have an item");
        }

        BlockPos lastPos = null;
        if(nbt.contains("LastKnownPos")) {
            lastPos = NbtUtils.readBlockPos(nbt.getCompound("LastKnownPos"));
        }

        IWireEndpoint endpoint1 = null, endpoint2 = null;
        if(nbt.contains("Endpoint1")) {
            endpoint1 = WireEndpointType.deserialize(nbt.getCompound("Endpoint1"));
        }

        if(nbt.contains("Endpoint2")) {
            endpoint2 = WireEndpointType.deserialize(nbt.getCompound("Endpoint2"));
        }

        var currentPos = blockPosition();
        if(lastPos != null && !lastPos.equals(currentPos)) {
            var diff = currentPos.subtract(lastPos);
            if(endpoint1 != null)
                endpoint1 = endpoint1.makeOffset(diff);
            if(endpoint2 != null)
                endpoint2 = endpoint2.makeOffset(diff);
        }

        setEndpoint1(endpoint1);
        setEndpoint2(endpoint2);

        entityData.set(TEMPERATURE, nbt.getFloat("Temperature"));
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setColor(DyeColor color) {
        var rgb = color.getTextureDiffuseColors();
        this.color = ((int) (rgb[0] * 255) << 16) | ((int) (rgb[1] * 255) << 8) | (int) (rgb[2] * 255);
    }

    public void setItem(WireItem item, int count) {
        this.item = item;
        this.itemCount = count;

        int thermalCount = Math.max(itemCount, 1);
        thermalMass = item.getThermalMass() * thermalCount;
        dissipationFactor = item.getDissipationFactor() * thermalCount;
        resistanceOverride = null;
    }

    public float getResistance() {
        if(resistanceOverride == null)
            resistanceOverride = item.getResistance() * Math.max(itemCount, 1);
        return resistanceOverride;
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return new ItemStack(item, 1);
    }

    public abstract void makeWire();
    public abstract void dropWire();

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
        if(item.canBeColored())
            nbt.putInt("Color", color);

        nbt.put("LastKnownPos", NbtUtils.writeBlockPos(blockPosition()));

        nbt.putFloat("Temperature", entityData.get(TEMPERATURE));
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
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

    private void cut(Player player, boolean correctTool) {
        ModdedSoundEvents.WIRE_CUT.playAt(level(), position(), 0.75f, 1.25f, false);
        double I = Math.abs(current());
        float threshold = ModdedConfigs.server().electricity.wireCutDamageCurrentThreshold.getF();
        if(!correctTool) {
            itemCount = (int) (itemCount * 0.75f);
            threshold *= 0.5f;
        }
        if(I >= threshold) {
            float damage = (float) (I / threshold);
            var registry = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            var source = new DamageSource(registry.getHolder(ModdedDamageTypes.LIVE_WIRE_CUTTING).get());
            player.hurt(source, damage);
        }
        kill();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if(stack.is(ModdedTags.Item.WIRE_CUTTERS.tag)) {
            cut(player, true);
            return InteractionResult.SUCCESS;
        } else if(stack.is(ModdedTags.Item.BAD_WIRE_CUTTERS.tag)) {
            cut(player, false);
            return InteractionResult.SUCCESS;
        } else if(stack.getItem() instanceof MultimeterItem multimeter) {
            return multimeter.useOnWire(player, stack, hand, this);
        } else if(stack.getItem() instanceof DyeItem dye) {
            if(item.canBeColored()) {
                setColor(dye.getDyeColor());
                return InteractionResult.SUCCESS;
            }
        } else if(stack.getItem() instanceof DebugItem debugger) {
            return debugger.useOn(this, player, hand);
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
        resistanceOverride = null;
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

    public int getColor() {
        if(item.canBeColored())
            return color;
        return -1;
    }

    public boolean isInsulated() {
        return item.isInsulated();
    }

    protected abstract void unloaded();

    public static void entityUnload(Entity entity, ServerLevel world) {
        if(entity instanceof BaseWireEntity wire)
            wire.unloaded();
    }
}
