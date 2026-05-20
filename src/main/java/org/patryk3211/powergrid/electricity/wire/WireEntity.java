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

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;

public abstract class WireEntity extends BaseWireEntity {
    private ElectricWire wire;

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
    public float current() {
        return wire == null || !wire.isConverged() ? 0 : (float) wire.current();
    }

    @Override
    public float measuredCurrent() {
        return Math.abs(current());
    }

    @Override
    public void makeWire() {
        var world = level();

        dropWire();

        // Cannot make a wire unless both endpoints are valid.
        if(endpoint1 == null || endpoint2 == null)
            return;

        if(!endpoint1.isValid(world) || !endpoint2.isValid(world))
            return;

        try {
            wire = GlobalElectricNetworks.makeConnection(world, endpoint1, endpoint2, this, new WorldNetworks.SimpleId(getUUID()));
        } catch(RuntimeException e) {
            PowerGrid.LOGGER.error("Failed to create wire for entity", e);
            kill();
        }
    }

    @Override
    public void dropWire() {
        if(wire != null) {
            wire.remove();
            wire = null;
        }
    }

    protected boolean isConnectedTo(LivingEntity entity) {
        if(entity instanceof Player player) {
            if(player.isSpectator())
                return false;
            return !player.isCreative() || ModdedConfigs.server().electricity.creativePlayerShortsWires.get();
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        var world = level();
        if(!world.isClientSide && ModdedConfigs.server().electricity.entityWireInteraction.get() && !isInsulated()) {
            var living = world.getEntitiesOfClass(LivingEntity.class, getBoundingBox(), this::isConnectedTo);
            EntityWireInteraction.wireTouching(this, living);
        }
    }

    @Override
    public void unloaded() {
        if(wire instanceof TransmissionLinePart part) {
            var reason = getRemovalReason();
            if(reason != null && reason.shouldDestroy()) {
                dropWire();
            } else {
                part.unload();
                wire = null;
            }
        } else {
            dropWire();
        }
    }
}
