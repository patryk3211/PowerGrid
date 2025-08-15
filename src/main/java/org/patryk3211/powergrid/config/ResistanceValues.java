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

import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

public class ResistanceValues {
    private static final List<Provider> providers = new ArrayList<>();

    public static float get(Block block) {
        for(var provider : providers) {
            var v = provider.get(block);
            if(v != null)
                return (float) v.getAsDouble();
        }
        throw new IllegalArgumentException("Block not found in any resistance providers");
    }

    public static float get(Block block, String suffix) {
        for(var provider : providers) {
            var v = provider.get(block, suffix);
            if(v != null)
                return (float) v.getAsDouble();
        }
        throw new IllegalArgumentException("Block with suffix '" + suffix + "' not found in any resistance providers");
    }

    public static void register(Provider provider) {
        providers.add(provider);
    }

    public interface Provider {
        @Nullable
        DoubleSupplier get(Block block);

        @Nullable
        DoubleSupplier get(Block block, String suffix);
    }
}
