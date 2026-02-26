package org.patryk3211.powergrid.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.patryk3211.powergrid.mixin.ChunkMapAccessor;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class PlayerSelectionImpl extends PlayerSelection {
    private final Consumer<ClientboundCustomPayloadPacket> sender;

    private PlayerSelectionImpl(Consumer<ClientboundCustomPayloadPacket> sender) {
        this.sender = sender;
    }

    @Override
    public void accept(ResourceLocation id, FriendlyByteBuf buffer) {
        CustomPayloadWrapper payload = CustomPayloadWrapper.create(id, buffer);
        ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
        sender.accept(packet);
    }

    public static PlayerSelection all() {
        return new PlayerSelectionImpl(packet -> {
            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                player.connection.send(packet);
            }
        });
    }

    public static PlayerSelection allWith(Predicate<ServerPlayer> condition) {
        return new PlayerSelectionImpl(packet -> {
            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (condition.test(player)) {
                    player.connection.send(packet);
                }
            }
        });
    }

    public static PlayerSelection of(ServerPlayer player) {
        return new PlayerSelectionImpl(packet -> player.connection.send(packet));
    }

    public static PlayerSelection tracking(Entity entity) {
        return new PlayerSelectionImpl(packet -> {
            ServerChunkCache manager = (ServerChunkCache) entity.level().getChunkSource();
            ChunkMap storage = manager.chunkMap;
            Object trackedEntity = ((ChunkMapAccessor)storage).getEntityMap().get(entity.getId());

            if (trackedEntity == null)
                return;

            for (ServerPlayerConnection connection : ((ChunkMapAccessor.TrackedEntityAccessor) trackedEntity).getSeenBy()) {
                connection.send(packet);
            }
        });
    }

    public static PlayerSelection trackingWith(Entity entity, Predicate<ServerPlayer> condition) {
        return new PlayerSelectionImpl(packet -> {
            ServerChunkCache manager = (ServerChunkCache) entity.level().getChunkSource();
            ChunkMap storage = manager.chunkMap;
            Object trackedEntity = ((ChunkMapAccessor)storage).getEntityMap().get(entity.getId());

            if (trackedEntity == null)
                return;

            for (ServerPlayerConnection connection : ((ChunkMapAccessor.TrackedEntityAccessor) trackedEntity).getSeenBy()) {
                if (condition.test(connection.getPlayer())) {
                    connection.send(packet);
                }
            }
        });
    }
    public static PlayerSelection tracking(BlockEntity be) {
        LevelChunk chunk = be.getLevel().getChunkAt(be.getBlockPos());
        return new PlayerSelectionImpl(packet -> {
            ServerChunkCache manager = (ServerChunkCache) be.getLevel().getChunkSource();
            manager.chunkMap.getPlayers(chunk.getPos(), false).forEach(player -> player.connection.send(packet));
        });
    }

    public static PlayerSelection tracking(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return new PlayerSelectionImpl(packet -> {
            ServerChunkCache manager = (ServerChunkCache) level.getChunkSource();
            manager.chunkMap.getPlayers(chunk.getPos(), false).forEach(player -> player.connection.send(packet));
        });
    }

    public static PlayerSelection trackingAndSelf(ServerPlayer player) {
        return new PlayerSelectionImpl(packet -> {
            player.connection.send(packet);
            // Also send to all tracking players
            ServerChunkCache manager = (ServerChunkCache) player.level().getChunkSource();
            ChunkMap storage = manager.chunkMap;
            Object trackedEntity = ((ChunkMapAccessor)storage).getEntityMap().get(player.getId());

            if (trackedEntity != null) {
                for (ServerPlayerConnection connection : ((ChunkMapAccessor.TrackedEntityAccessor) trackedEntity).getSeenBy()) {
                    connection.send(packet);
                }
            }
        });
    }
}
