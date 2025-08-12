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
package org.patryk3211.powergrid.utility.fabric;

import com.tterrag.registrate.builders.EntityBuilder;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import org.patryk3211.powergrid.utility.EntityProperties;

public class EntityPropertiesImpl implements EntityProperties {
    public static <T extends Entity, P> NonNullUnaryOperator<EntityBuilder<T, P>> apply(NonNullConsumer<EntityProperties> consumer) {
        return b -> b.properties(typeBuilder -> consumer.accept(new EntityPropertiesImpl(typeBuilder)));
    }

    private final FabricEntityTypeBuilder<?> builder;

    private EntityPropertiesImpl(FabricEntityTypeBuilder<?> builder) {
        this.builder = builder;
    }

    @Override
    public EntityProperties dimensions(float width, float height) {
        builder.dimensions(EntityDimensions.fixed(width, height));
        return this;
    }

    @Override
    public EntityProperties trackRangeChunks(int range) {
        builder.trackRangeChunks(range);
        return this;
    }

    @Override
    public EntityProperties trackedUpdateRate(int rate) {
        builder.trackedUpdateRate(rate);
        return this;
    }

    @Override
    public EntityProperties forceTrackedVelocityUpdates(boolean force) {
        builder.forceTrackedVelocityUpdates(force);
        return this;
    }
}
