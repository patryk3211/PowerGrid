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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
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
        return wire == null ? 0 : wire.current();
    }

    @Override
    public void tick() {
        super.tick();

        if((deferEndpointResolution & 1) != 0) {
            if(endpoint1 != null && endpoint1.isValid(level())) {
                endpoint1.assignWireEntity(this);
                deferEndpointResolution &= ~1;
                makeWire();
            }
        }
        if((deferEndpointResolution & 2) != 0) {
            if(endpoint2 != null && endpoint2.isValid(level())) {
                endpoint2.assignWireEntity(this);
                deferEndpointResolution &= ~2;
                makeWire();
            }
        }
    }

    @Override
    public void makeWire() {
        // Client doesn't make a wire, connections are handled differently.
        var world = level();
        if(world.isClientSide && !(world instanceof PonderLevel))
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

    @Override
    public void dropWire() {
        if(wire != null) {
            wire.remove();
            wire = null;
        }
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
