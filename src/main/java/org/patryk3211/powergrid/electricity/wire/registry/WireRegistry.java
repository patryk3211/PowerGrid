package org.patryk3211.powergrid.electricity.wire.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;

public class WireRegistry {
    public static final ResourceKey<Registry<WireItemEntry>> KEY = ResourceKey.createRegistryKey(PowerGrid.asResource("wire_types"));

    @Contract("_, null -> null")
    public static WireItemEntry forItem(@NotNull Level level, Item item) {
        if(item == null)
            return null;
        return level.registryAccess()
                .registryOrThrow(KEY)
                .get(getId(item));
    }

    public static ResourceLocation getId(@NotNull Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
