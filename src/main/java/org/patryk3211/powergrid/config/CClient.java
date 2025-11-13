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

    @Override
    public String getName() {
        return "common";
    }

    private static class Comments {
        public static final String crtRed = "Amount of red in the CRT screen glow";
        public static final String crtGreen = "Amount of green in the CRT screen glow";
        public static final String crtBlue = "Amount of blue in the CRT screen glow";
    }
}
