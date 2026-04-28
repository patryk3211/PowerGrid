package org.patryk3211.powergrid.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
    @Accessor
    Int2ObjectMap<?> getEntityMap();

    @Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
    interface TrackedEntityAccessor {
        @Accessor
        Set<ServerPlayerConnection> getSeenBy();
    }
}
