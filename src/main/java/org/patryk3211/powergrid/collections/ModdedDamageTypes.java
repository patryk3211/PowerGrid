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

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.PowerGrid;

public enum ModdedDamageTypes {
    OVERLOADED_MACHINE("overloaded_machine"),
    ZAP("zap"),
    LIVE_WIRE_CUTTING("live_wire_cutting"),
    ELECTROCUTION("electrocution"),
    SPINNING_ROTOR("spinning_rotor"),
    ACID("acid");

    public final ResourceKey<DamageType> key;

    ModdedDamageTypes(String id) {
        key = net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, PowerGrid.asResource(id));
    }

    public Holder<DamageType> holder(Level level) {
        var registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return registry.getHolderOrThrow(key);
    }

    public Holder<DamageType> holder(MinecraftServer server) {
        var registry = server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return registry.getHolderOrThrow(key);
    }

    public DamageSource simpleDamageSource(Level level) {
        return new DamageSource(holder(level));
    }

    public DamageSource simpleDamageSource(MinecraftServer server) {
        return new DamageSource(holder(server));
    }
}
