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
    public final ConfigInt solverSimpleMaxIterations = i(200, "solverSimpleMaxIterations", Comments.solverSimpleMaxIterations);
    public final ConfigInt solverComplexMaxIterations = i(200, "solverComplexMaxIterations", Comments.solverComplexMaxIterations);
    public final ConfigInt stateSynchronization = i(80, 0, "stateSynchronizationInterval", Comments.stateSynchronization);

    @Override
    public String getName() {
        return "common";
    }

    private static class Comments {
        public static final String lotsOfLogs = "Enables extensive logging in different segments of the mod (can cause larger log files and log spam)";
        public static final String solverSimpleMaxIterations = "Maximum solver iterations for networks without dynamic residuals";
        public static final String solverComplexMaxIterations = "Maximum solver iterations for networks with dynamic residuals";
        public static final String stateSynchronization = "Periodic state synchronization sent by the server. This option makes sure that the clients are always close to the server simulation state, however it can sometimes cause issues when synchronizing certain circuit (0 = disabled)";
    }
}
