/*
 * Copyright 2026 patryk3211
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

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.light.string.PatternData;
import org.patryk3211.powergrid.electricity.wire.WireConnection;
import org.patryk3211.powergrid.equipment.BoostData;

public class ModdedDataComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTER = DeferredRegister.create(PowerGrid.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<PatternData>> LIGHT_PATTERN = persistent("pattern", PatternData.CODEC);
    public static final RegistrySupplier<DataComponentType<WireConnection>> CONNECTION_DATA = persistent("connection", WireConnection.CODEC);

    public static final RegistrySupplier<DataComponentType<BoostData>> BOOST = persistent("boost", BoostData.CODEC);

    public static final RegistrySupplier<DataComponentType<BlockPos>> WINDING_CONNECTION = persistent("winding_connection", BlockPos.CODEC);

    public static final RegistrySupplier<DataComponentType<Integer>> PORTABLE_BATTERY_CHARGE = persistent("portable_battery_charge", Codec.INT);

    public static <T> RegistrySupplier<DataComponentType<T>> persistent(String id, Codec<T> codec) {
        return REGISTER.register(id, () -> DataComponentType.<T>builder().persistent(codec).build());
    }
}
