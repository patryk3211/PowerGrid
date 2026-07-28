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
package org.patryk3211.powergrid.electricity.wire.powercord;

import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;

public interface ICordEndpoint extends IWireEndpoint {
    IWireEndpoint getEndpoint1();
    IWireEndpoint getEndpoint2();

    @Override
    default <T extends BaseWireEntity> boolean canAcceptType(Class<T> clazz) {
        return CordEntity.class.isAssignableFrom(clazz);
    }

    @Override
    default boolean isStructurallyValid(Level world) {
        var endpoint1 = getEndpoint1();
        var endpoint2 = getEndpoint2();
        return endpoint1 != null
                && endpoint1.isStructurallyValid(world)
                && endpoint2 != null
                && endpoint2.isStructurallyValid(world);
    }

    @Override
    default void assignWireEntity(BaseWireEntity entity) {
        getEndpoint1().assignWireEntity(entity);
        getEndpoint2().assignWireEntity(entity);
    }

    @Override
    default void removeWireEntity(BaseWireEntity entity) {
        getEndpoint1().removeWireEntity(entity);
        getEndpoint2().removeWireEntity(entity);
    }
}
