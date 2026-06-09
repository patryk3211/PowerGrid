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
import net.minecraft.world.entity.EntityType;
import org.patryk3211.powergrid.PowerGrid;

public class ModdedTags {
    public static final String FORGE_NAMESPACE = "c";
    public static final String SABLE_NAMESPACE = "sable";

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
            tag = itemTag(ResourceLocation.fromNamespaceAndPath(namespace, name));
        }
    }

    public enum Block {
        SILVER_ORES(FORGE_NAMESPACE, "silver_ores"),
        AFFECTED_BY_LAMP("affected_by_lamp"),
        IGNORE_IN_ROTOR_ASSEMBLY_SIZE("ignore_in_rotor_assembly_size"),
        CONDUCTIVE_GROUND("conductive_ground"),
        CARBON_PILE_BLOCK("carbon_pile_block"),

        SABLE_QUARTER_VOLUME(SABLE_NAMESPACE, "quarter_volume"),
        SABLE_HALF_VOLUME(SABLE_NAMESPACE, "half_volume"),

        SABLE_SUPER_LIGHT(SABLE_NAMESPACE, "super_light"),
        SABLE_LIGHT(SABLE_NAMESPACE, "light"),
        SABLE_HEAVY(SABLE_NAMESPACE, "heavy"),
        SABLE_SUPER_HEAVY(SABLE_NAMESPACE, "super_heavy"),
        GLASS_BLOCK(FORGE_NAMESPACE, "glass"),
        GLASS_PANE(FORGE_NAMESPACE, "glass_panes");

        public final TagKey<net.minecraft.world.level.block.Block> tag;

        Block(String name) {
            this(PowerGrid.MOD_ID, name);
        }

        Block(String namespace, String name) {
            tag = blockTag(ResourceLocation.fromNamespaceAndPath(namespace, name));
        }
    }

    public enum Entity {
        RETAIN_IN_SUB_LEVEL(SABLE_NAMESPACE, "retain_in_sub_level");

        public final TagKey<EntityType<?>> tag;

        Entity(String name) {
            this(PowerGrid.MOD_ID, name);
        }

        Entity(String namespace, String name) {
            tag = entityTag(ResourceLocation.fromNamespaceAndPath(namespace, name));
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

    @ExpectPlatform
    public static TagKey<EntityType<?>> entityTag(ResourceLocation id) {
        throw new AssertionError();
    }

    public static TagKey<net.minecraft.world.item.Item> forgeItemTag(String path) {
        return itemTag(ResourceLocation.fromNamespaceAndPath(FORGE_NAMESPACE, path));
    }

    public static TagKey<net.minecraft.world.level.block.Block> forgeBlockTag(String path) {
        return blockTag(ResourceLocation.fromNamespaceAndPath(FORGE_NAMESPACE, path));
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
