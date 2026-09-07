package org.patryk3211.powergrid.electricity.solarpanel.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;

public class SolarBiomeRegistry {
    public static final ResourceKey<Registry<SolarBiomeEntry>> KEY = ResourceKey.createRegistryKey(PowerGrid.asResource("solar_biome_override"));

    public static SolarBiomeEntry forBiome(@NotNull Level level, BlockPos pos) {
        if (pos == null) return null;
        var test = level.registryAccess()
                .registryOrThrow(KEY);
        SolarBiomeEntry result = null;
        for (var i = 0; i < test.size(); ++i) {
            var holder = test.getHolder(i);
            if (holder.isPresent()){
                var biomeEntry = holder.get().value();
                var biomeName = level.getBiome(pos).unwrap().map((resourceKey) ->
                        resourceKey.location().toString(), (biome) -> "[unregistered " + biome + "]");
                result = biomeEntry.biome().equals(biomeName) ? biomeEntry : null;
            }
        }
        return result;
    }
}
