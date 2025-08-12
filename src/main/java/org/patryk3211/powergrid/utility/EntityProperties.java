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
package org.patryk3211.powergrid.utility;

import com.tterrag.registrate.builders.EntityBuilder;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.Entity;

public interface EntityProperties {
    @ExpectPlatform
    static <T extends Entity, P> NonNullUnaryOperator<EntityBuilder<T, P>> apply(NonNullConsumer<EntityProperties> consumer) {
        throw new AssertionError();
    }

    EntityProperties dimensions(float width, float height);
    EntityProperties trackRangeChunks(int range);
    EntityProperties trackedUpdateRate(int rate);
    EntityProperties forceTrackedVelocityUpdates(boolean force);
}
