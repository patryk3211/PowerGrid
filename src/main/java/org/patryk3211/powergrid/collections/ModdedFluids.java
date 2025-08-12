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
package org.patryk3211.powergrid.collections;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.fluid.Fluid;

public class ModdedFluids {
    @ExpectPlatform
    public static Fluid acid() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Fluid acidFlowing() {
        throw new AssertionError();
    }

    public static void register() {
        platformInit();
    }

    @ExpectPlatform
    public static void platformInit() {
        throw new AssertionError();
    }
}
