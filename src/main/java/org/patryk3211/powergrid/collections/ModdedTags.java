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

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.patryk3211.powergrid.PowerGrid;

public class ModdedTags {
    public static final String FORGE_NAMESPACE = PowerGrid.forPlatform("c", "forge");

    public enum Item {
        RAW_ORES(FORGE_NAMESPACE, "raw_ores"),
        PLATES(FORGE_NAMESPACE, "plates"),
        WIRES(FORGE_NAMESPACE, "wires"),
        LIGHT_WIRES("light_wires"),
        COILS(FORGE_NAMESPACE, "coils"),
        CIRCUIT_SCHEMATIC_HOLDER("circuit_schematic_holder"),
        CIRCUIT_COMPONENT("circuit_component"),
        FUSE_RESETTING("fuse_resetting")
        ;

        public final TagKey<net.minecraft.world.item.Item> tag;

        Item(String name) {
            this(PowerGrid.MOD_ID, name);
        }

        Item(String namespace, String name) {
            tag = itemTag(new ResourceLocation(namespace, name));
        }
    }

    public enum Block {
        SILVER_ORES(FORGE_NAMESPACE, "silver_ores"),
        AFFECTED_BY_LAMP("affected_by_lamp"),
        IGNORE_IN_ROTOR_ASSEMBLY_SIZE("ignore_in_rotor_assembly_size"),
        CONDUCTIVE_GROUND("conductive_ground"),
        CARBON_PILE_BLOCK("carbon_pile_block"),
        GLASS_BLOCK(FORGE_NAMESPACE, "glass"),
        GLASS_PANE(FORGE_NAMESPACE, "glass_panes");

        public final TagKey<net.minecraft.world.level.block.Block> tag;

        Block(String name) {
            this(PowerGrid.MOD_ID, name);
        }

        Block(String namespace, String name) {
            tag = blockTag(new ResourceLocation(namespace, name));
        }
    }

    @ExpectPlatform
    public static TagKey<net.minecraft.world.level.block.Block> blockTag(ResourceLocation id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static TagKey<net.minecraft.world.item.Item> itemTag(ResourceLocation id) {
        throw new AssertionError();
    }

    public static TagKey<net.minecraft.world.item.Item> forgeItemTag(String path) {
        return itemTag(new ResourceLocation(FORGE_NAMESPACE, path));
    }

    public static TagKey<net.minecraft.world.level.block.Block> forgeBlockTag(String path) {
        return blockTag(new ResourceLocation(FORGE_NAMESPACE, path));
    }

    public static TagKey<net.minecraft.world.item.Item> plates(String ingot) {
        return forgeItemTag(PowerGrid.forPlatform(ingot + "_plates", "plates/" + ingot));
    }

    public static TagKey<net.minecraft.world.item.Item> nuggets(String ingot) {
        return forgeItemTag(PowerGrid.forPlatform(ingot + "_nuggets", "nuggets/" + ingot));
    }

    public static TagKey<net.minecraft.world.item.Item> ingots(String ingot) {
        return forgeItemTag(PowerGrid.forPlatform(ingot + "_ingots", "ingots/" + ingot));
    }

    public static TagKey<net.minecraft.world.item.Item> wires(String ingot) {
        return forgeItemTag(PowerGrid.forPlatform(ingot + "_wires", "wires/" + ingot));
    }
}
