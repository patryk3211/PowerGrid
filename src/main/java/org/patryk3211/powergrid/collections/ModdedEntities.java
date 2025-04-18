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

import com.tterrag.registrate.util.entry.EntityEntry;
import net.minecraft.entity.SpawnGroup;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.electricity.wire.WireRenderer;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedEntities {
    public static final EntityEntry<WireEntity> WIRE =
            REGISTRATE.entity("wire", WireEntity::new, SpawnGroup.MISC)
                    .renderer(() -> WireRenderer::new)
                    .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
