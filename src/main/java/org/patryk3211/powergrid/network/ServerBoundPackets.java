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
package org.patryk3211.powergrid.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.patryk3211.powergrid.network.packets.BlockWireAttachC2SPacket;
import org.patryk3211.powergrid.network.packets.BlockWireCutC2SPacket;

public class ServerBoundPackets {
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(BlockWireCutC2SPacket.TYPE, BlockWireCutC2SPacket::handler);
        ServerPlayNetworking.registerGlobalReceiver(BlockWireAttachC2SPacket.TYPE, BlockWireAttachC2SPacket::handler);
    }
}
