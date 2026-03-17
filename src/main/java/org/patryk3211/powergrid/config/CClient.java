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

public class CClient extends ConfigBase {
    public final ConfigInt crtRed = i(64, 0, 255, "crtRed", Comments.crtRed);
    public final ConfigInt crtGreen = i(4, 0, 255, "crtGreen", Comments.crtGreen);
    public final ConfigInt crtBlue = i(2, 0, 255, "crtBlue", Comments.crtBlue);

    public final ConfigInt crtPointCount = i(100, 10, "crtPointCount", Comments.crtPointCount);
    public final ConfigFloat crtTracePersistence = f(0.025f, 0, "crtTracePersistence", Comments.crtTracePersistence);
    public final ConfigFloat crtDotSize = f(1/32f, 0, "crtDotSize", Comments.crtDotSize);

    public final ConfigFloat crtZDepth = f(0.0001f, 0, "crtZDepth", Comments.crtZDepth);

    public final ConfigBool wireLOD = b(true, "wireLOD", Comments.wireLOD);

    public final ConfigFloat hummingSoundMultiplier = f(1.0f, 0.0f, "hummingSoundMultiplier", Comments.hummingSoundMultiplier);
    public final ConfigFloat generatorSoundMultiplier = f(1.0f, 0.0f, "generatorSoundMultiplier", Comments.generatorSoundMultiplier);

    @Override
    public String getName() {
        return "common";
    }

    private static class Comments {
        public static final String crtRed = "Amount of red in the CRT screen glow";
        public static final String crtGreen = "Amount of green in the CRT screen glow";
        public static final String crtBlue = "Amount of blue in the CRT screen glow";

        public static final String crtPointCount = "Controls how many individual points the CRT trace can have (1 point is added per tick)";
        public static final String crtTracePersistence = "Controls the slope of trace brightness decay (lower value means faster decay)";
        public static final String crtDotSize = "Controls the CRT trace thickness";
        public static final String crtZDepth = "Controls how much the CRT trace falls as it fades. This values shouldn't be too small or it can cause Z fighting";

        public static final String wireLOD = "Enable decreased level of detail rendering of far away wires";

        public static final String hummingSoundMultiplier = "Multiplier for all humming ambient sounds";
        public static final String generatorSoundMultiplier = "Multiplier for generator ambient sound";
    }
}
