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
package org.patryk3211.powergrid.config;

import net.createmod.catnip.config.ConfigBase;

public class CCommon extends ConfigBase {
    public final ConfigBool lotsOfLogs = b(false, "lotsOfLogs", Comments.lotsOfLogs);
    public final ConfigInt stateSynchronization = i(100, 0, "fullStateSynchronizationInterval", Comments.stateSynchronization);
    public final ConfigBool syncWithDoubles = b(true, "syncWithDoubles", Comments.syncWithDoubles);
    public final ConfigBool allocateUnpooledBuffers = b(false, "allocateUnpooledBuffers");

    @Override
    public String getName() {
        return "common";
    }

    private static class Comments {
        public static final String lotsOfLogs = "Enables extensive logging in different segments of the mod (can cause larger log files and log spam)";
        public static final String stateSynchronization = "Periodic state synchronization sent by the server. This option makes sure that the clients are always close to the server simulation state, it will send the NBT data of all block entities in an electrical network to all clients (0 = disabled)";
        public static final String syncWithDoubles = "Synchronize network with double precision numbers (this will double the amount of data that needs to be sent)";
    }
}
