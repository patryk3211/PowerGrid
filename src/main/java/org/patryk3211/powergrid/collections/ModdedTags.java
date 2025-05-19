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

import com.simibubi.create.AllTags;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;

public class ModdedTags {
    public enum Item {
        COIL_WIRE("coil_wire");

        public final TagKey<net.minecraft.item.Item> tag;

        Item(String name) {
            this(PowerGrid.MOD_ID, name);
        }

        Item(String namespace, String name) {
            tag = AllTags.optionalTag(Registries.ITEM, new Identifier(namespace, name));
        }
    }
}
