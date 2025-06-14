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

import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;

public class ModdedPackets {
    public static final Identifier ENTITY_DATA_PACKET = new Identifier(PowerGrid.MOD_ID, "entity_data");
    public static final Identifier AGGREGATE_COILS_PACKET = new Identifier(PowerGrid.MOD_ID, "aggregate_coils");

    public static final Identifier BLOCK_WIRE_CUT = PowerGrid.asResource("block_wire_cut");
    public static final Identifier BLOCK_WIRE_ATTACH = PowerGrid.asResource("block_wire_attach");
}
