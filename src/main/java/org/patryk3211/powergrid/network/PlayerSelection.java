package org.patryk3211.powergrid.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.swing.text.html.parser.Entity;
import java.util.function.Predicate;

public abstract class PlayerSelection {
    public abstract void accept(ResourceLocation id, FriendlyByteBuf buffer);	public static PlayerSelection all() {
        return PlayerSelectionImpl.all();
    }	public static PlayerSelection allWith(Predicate<ServerPlayer> condition) {
        return PlayerSelectionImpl.allWith(condition);
    }	public static PlayerSelection of(ServerPlayer player) {
        return PlayerSelectionImpl.of(player);
    }	public static PlayerSelection tracking(Entity entity) {
        return PlayerSelectionImpl.tracking(entity);
    }	public static PlayerSelection trackingWith(Entity entity, Predicate<ServerPlayer> condition) {
        return PlayerSelectionImpl.trackingWith(entity, condition);
    }	public static PlayerSelection tracking(BlockEntity be) {
        return PlayerSelectionImpl.tracking(be);
    }	public static PlayerSelection tracking(ServerLevel level, BlockPos pos) {
        return PlayerSelectionImpl.tracking(level, pos);
    }	public static PlayerSelection trackingAndSelf(ServerPlayer player) {
        return PlayerSelectionImpl.trackingAndSelf(player);
    }
}
