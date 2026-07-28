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
package org.patryk3211.powergrid.electricity.wire;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;

public interface IWireEndpoint {
    WireEndpointType type();

    void read(CompoundTag nbt);
    void write(CompoundTag nbt);

    @NotNull
    Vec3 getExactPosition(Level world);

    // TODO: Implement for other endpoints
    default boolean isValid(Level world) {
        return true;
    }

    /**
     * Checks whether the physical endpoint still exists independently of
     * runtime electrical-behaviour initialization.
     *
     * <p>Most endpoint types have no separate late-initialization phase, so
     * their normal validity is also their structural validity.</p>
     */
    default boolean isStructurallyValid(Level world) {
        return isValid(world);
    }

    default <T extends BaseWireEntity> boolean canAcceptType(Class<T> clazz) {
        return false;
    }

    default OwnedFloatingNode getNode(Level world) {
        throw new IllegalStateException("Cannot fetch node");
    }

    default void joinNetwork(Level world, ElectricalNetwork network) {
        throw new IllegalStateException("Cannot join network");
    }

    default void assignWireEntity(BaseWireEntity entity) {
        throw new IllegalStateException("Cannot assign a wire entity");
    }

    default void removeWireEntity(BaseWireEntity entity) {
        throw new IllegalStateException("Cannot remove a wire entity");
    }

    default void moveWireEntity(BaseWireEntity entity) {
        removeWireEntity(entity);
    }

    default CompoundTag serialize() {
        return type().serialize(this);
    }

    default IWireEndpoint makeOffset(BlockPos offset) {
        return null;
    }

    default IWireEndpoint makeOffset(BlockPos blockOffset, Vec3 offset) {
        return makeOffset(blockOffset);
    }

    default SubLevelAccess getSubLevel(Level world) {
        return SableCompanion.INSTANCE.getContaining(world, getExactPosition(world));
    }
}
