package org.patryk3211.powergrid.collections;

import com.google.common.collect.Sets;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.advancements.PowerGridAdvancement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class ModdedAdvancements implements DataProvider {
    public static final List<PowerGridAdvancement> ENTRIES = new ArrayList<>();

    private static PowerGridAdvancement create(String id, UnaryOperator<PowerGridAdvancement.Builder> b) {
        return new PowerGridAdvancement(id, b);
    }

    private final PackOutput output;

    public ModdedAdvancements(PackOutput output) {
        this.output = output;
    }


    @NotNull
    @Override
    public CompletableFuture<?> run(@NotNull CachedOutput cache) {
        PackOutput.PathProvider pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancements");
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Set<ResourceLocation> set = Sets.newHashSet();
        Consumer<Advancement> consumer = (advancementx) -> {
            ResourceLocation id = advancementx.getId();
            if (!set.add(id)) {
                throw new IllegalStateException("Duplicate advancement " + id);
            } else {
                Path path = pathProvider.json(id);
                futures.add(DataProvider.saveStable(cache, advancementx.deconstruct().serializeToJson(), path));
            }
        };

        for(var advancement : ENTRIES) {
            advancement.save(consumer);
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public String getName() {
        return "Power Grid's Advancements";
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        for(var advancement : ENTRIES) {
            advancement.provideLang(consumer);
        }
    }

    public static void register() { }
}
