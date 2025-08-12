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

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import org.patryk3211.powergrid.PowerGrid;

public class ModdedDamageTypes {
    public static final ResourceKey<DamageType> OVERLOADED_MACHINE = ResourceKey.create(Registries.DAMAGE_TYPE, PowerGrid.asResource("overloaded_machine"));
    public static final ResourceKey<DamageType> ZAP = ResourceKey.create(Registries.DAMAGE_TYPE, PowerGrid.asResource("zap"));
}
