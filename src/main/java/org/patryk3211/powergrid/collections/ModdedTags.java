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
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentRegistry;

public class ModdedTags {
    public static final String FORGE_NAMESPACE = "c";

    public enum Item {
        COIL_WIRE("coil_wire"),
        SILVER_INGOTS(FORGE_NAMESPACE, "silver_ingots"),
        SILVER_ORES(FORGE_NAMESPACE, "silver_ores"),
        RAW_ORES(FORGE_NAMESPACE, "raw_ores"),
        PLATES(FORGE_NAMESPACE, "plates"),
        WIRES("wires"),
        CATALYZERS("catalyzers"),
        LIGHT_WIRES("light_wires")
        ;

        public final TagKey<net.minecraft.item.Item> tag;

        Item(String name) {
            this(PowerGrid.MOD_ID, name);
        }

        Item(String namespace, String name) {
            tag = AllTags.optionalTag(Registries.ITEM, new Identifier(namespace, name));
        }
    }

    public enum Block {
        SILVER_ORES(FORGE_NAMESPACE, "silver_ores"),
        AFFECTED_BY_LAMP("affected_by_lamp")
        ;

        public final TagKey<net.minecraft.block.Block> tag;

        Block(String name) {
            this(PowerGrid.MOD_ID, name);
        }

        Block(String namespace, String name) {
            tag = AllTags.optionalTag(Registries.BLOCK, new Identifier(namespace, name));
        }
    }

    public enum Reagent {
        POWDER("powder")
        ;

        public final TagKey<org.patryk3211.powergrid.chemistry.reagent.Reagent> tag;

        Reagent(String name) {
            this(PowerGrid.MOD_ID, name);
        }

        Reagent(String namespace, String name) {
            tag = AllTags.optionalTag(ReagentRegistry.REGISTRY, new Identifier(namespace, name));
        }
    }
}
