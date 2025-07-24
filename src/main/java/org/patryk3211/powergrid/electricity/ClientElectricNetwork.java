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
package org.patryk3211.powergrid.electricity;

import io.github.fabricators_of_create.porting_lib.event.client.ClientWorldEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.patryk3211.powergrid.network.ClientBoundPackets;
import org.patryk3211.powergrid.network.packets.TransmissionLineS2CPacket;

@Environment(EnvType.CLIENT)
public class ClientElectricNetwork extends GlobalElectricNetworks {
    public static void init() {
        ClientTickEvents.START_WORLD_TICK.register(GlobalElectricNetworks::tick);
        ClientWorldEvents.UNLOAD.register((client, world) -> worldNetworks.remove(world));
    }

    public static void partialTrackedLine(TransmissionLineS2CPacket packet) {
        ((ClientWorldNetworks) getWorldNetworks(ClientBoundPackets.world())).partialLine(packet);
    }
}
