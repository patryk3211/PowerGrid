package org.patryk3211.powergrid.data;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedEntities;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class EntityHandlerProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final PackOutput output;

    public EntityHandlerProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path root = output.getOutputFolder(PackOutput.Target.DATA_PACK);

        return CompletableFuture.allOf(
                writeEntity(cache, root, ModdedEntities.HANGING_WIRE.get()),
                writeEntity(cache, root, ModdedEntities.BLOCK_WIRE.get()),
                writeEntity(cache, root, ModdedEntities.CORD_ENTITY.get()),
                writeEntity(cache, root, ModdedEntities.STRING_LIGHT_CORD.get())
        );
    }

    private CompletableFuture<?> writeEntity(CachedOutput cache, Path root, EntityType<?> entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity);
        String name = id.getPath();

        JsonObject json = new JsonObject();
        json.addProperty("handler", "valkyrienskies:shipyard");

        Path target = root.resolve(PowerGrid.MOD_ID + "/vs_entities/" + name + ".json");
        return DataProvider.saveStable(cache, json, target);
    }

    @Override
    public String getName() {
        return "Powergrid Entity Handlers";
    }
}