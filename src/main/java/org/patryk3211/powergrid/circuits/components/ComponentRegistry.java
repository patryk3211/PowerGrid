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
package org.patryk3211.powergrid.circuits.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;

public class ComponentRegistry {
    public static final ResourceKey<Registry<Component>> REGISTRY_KEY = ResourceKey.createRegistryKey(PowerGrid.asResource("components"));

    public static final ResourceKey<Registry<ComponentItemEntry>> ITEM_REGISTRY_KEY = ResourceKey.createRegistryKey(PowerGrid.asResource("component_items"));
    public static final Codec<ComponentItemEntry> ITEM_CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ComponentItemEntry::item)
            ).apply(instance, ComponentItemEntry::new));

    @ExpectPlatform
    public static Iterable<Component> entries() {
        throw new AssertionError();
    }

    @Contract("_, null -> null")
    public static Item getItem(@NotNull Level level, Component component) {
        if(component == null)
            return null;
        return level.registryAccess()
                .registryOrThrow(ITEM_REGISTRY_KEY)
                .get(getId(component)).item();
    }

    public static Component getComponent(@NotNull Level level, Item item) {
        var id = level.registryAccess()
                .registryOrThrow(ComponentRegistry.ITEM_REGISTRY_KEY)
                .getKey(new ComponentItemEntry(item));
        return id == null ? null : get(id);
    }

    @Environment(EnvType.CLIENT)
    public static Item getItem(Component component) {
        if(component == null)
            return null;
        return Minecraft.getInstance().getConnection().registryAccess()
                .registryOrThrow(ITEM_REGISTRY_KEY)
                .get(getId(component)).item();
    }

    @Environment(EnvType.CLIENT)
    public static Component getComponent(Item item) {
        var registry = Minecraft.getInstance().getConnection().registryAccess()
                .registryOrThrow(ComponentRegistry.ITEM_REGISTRY_KEY);
        for(var entry : registry.entrySet()) {
            if(entry.getValue().item() == item)
                return get(entry.getKey().location());
        }
        return null;
    }

    @NotNull
    @ExpectPlatform
    public static ResourceLocation getId(@NotNull Component component) {
        throw new AssertionError();
    }

    @NotNull
    @ExpectPlatform
    public static Component get(@NotNull ResourceLocation id) {
        throw new AssertionError();
    }
}
