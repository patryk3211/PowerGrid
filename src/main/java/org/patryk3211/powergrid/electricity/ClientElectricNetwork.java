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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.patryk3211.powergrid.network.ClientBoundPackets;
import org.patryk3211.powergrid.network.packets.TransmissionLineStateS2CPacket;

@Environment(EnvType.CLIENT)
public class ClientElectricNetwork extends GlobalElectricNetworks {
    public static void partialTrackedLine(TransmissionLineStateS2CPacket packet) {
        getWorldNetworks().partialLine(packet);
    }

    public static ClientWorldNetworks getWorldNetworks() {
        return (ClientWorldNetworks) getWorldNetworks(ClientBoundPackets.world());
    }

    public static void unloadWorld(Minecraft minecraftClient, ClientLevel world) {
        worldNetworks.remove(world);
    }
}
