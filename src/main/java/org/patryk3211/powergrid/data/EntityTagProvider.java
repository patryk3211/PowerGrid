package org.patryk3211.powergrid.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.collections.ModdedTags;

import java.util.concurrent.CompletableFuture;

public class EntityTagProvider extends TagsProvider<EntityType<?>> {
    public EntityTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ENTITY_TYPE, lookupProvider);
    }

    public ResourceKey<EntityType<?>> reverseLookup(EntityType<?> entity) {
        var key = BuiltInRegistries.ENTITY_TYPE.getResourceKey(entity);
        if(key.isEmpty())
            throw new IllegalArgumentException("Entity type is not registered");
        return key.get();
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModdedTags.Entity.RETAIN_IN_SUB_LEVEL.tag)
                .add(reverseLookup(ModdedEntities.BLOCK_WIRE.get()));
    }
}
