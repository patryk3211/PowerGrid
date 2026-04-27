package org.patryk3211.powergrid.network.packets;

import dev.architectury.networking.NetworkManager;
import it.unimi.dsi.fastutil.objects.Reference2BooleanArrayMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.Map;
import java.util.function.Supplier;

public class NegotiateSyncC2SPacket implements SimplePacket {
    public static final Map<ServerPlayer, Boolean> SYNC_TYPES = new Reference2BooleanArrayMap<>();

    private final boolean useDoubles;

    public NegotiateSyncC2SPacket(boolean useDoubles) {
        this.useDoubles = useDoubles;
    }

    public NegotiateSyncC2SPacket(FriendlyByteBuf buf) {
        useDoubles = buf.readBoolean();
    }

    public static boolean useDoubles(ServerPlayer player) {
        if(!ModdedConfigs.common().syncWithDoubles.get())
            return false;
        return SYNC_TYPES.getOrDefault(player, false);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(useDoubles);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            SYNC_TYPES.put((ServerPlayer) ctx.getPlayer(), useDoubles);
        });
    }
}
