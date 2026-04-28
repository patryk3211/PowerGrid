package org.patryk3211.powergrid.network;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.patryk3211.powergrid.PowerGrid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PacketSetImpl extends PacketSet {
    public static final Map<ResourceLocation, PacketSet> HANDLERS = new HashMap<>();

    protected PacketSetImpl(String id, int version,
                            List<Function<FriendlyByteBuf, S2CPacket>> s2cPackets,
                            Object2IntMap<Class<? extends S2CPacket>> s2cTypes,
                            List<Function<FriendlyByteBuf, C2SPacket>> c2sPackets,
                            Object2IntMap<Class<? extends C2SPacket>> c2sTypes) {
        super(id, version, s2cPackets, s2cTypes, c2sPackets, c2sTypes);
    }

    @Override
    public void registerS2CListener() {
        HANDLERS.put(s2cPacket, this);
    }

    @Override
    public void registerC2SListener() {
        HANDLERS.put(c2sPacket, this);
    }

    @Override
    public void send(Object packet) {
        throw new UnsupportedOperationException("Create packet forwarding is not wired for NeoForge 21 yet");
    }

    @Override
    public void sendTo(ServerPlayer player, Object packet) {
        throw new UnsupportedOperationException("Create packet forwarding is not wired for NeoForge 21 yet");
    }

    @Override
    public void sendTo(PlayerSelection selection, Object packet) {
        throw new UnsupportedOperationException("Create packet forwarding is not wired for NeoForge 21 yet");
    }

    @Override
    protected void doSendC2S(FriendlyByteBuf buf) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            CustomPayloadWrapper payload = CustomPayloadWrapper.create(c2sPacket, buf);
            connection.send(new ServerboundCustomPayloadPacket(payload));
        } else {
            PowerGrid.LOGGER.error("Cannot send a C2S packet before the client connection exists, skipping!");
        }
    }

    @ApiStatus.Internal
    public static PacketSet create(String id, int version,
                                   List<Function<FriendlyByteBuf, S2CPacket>> s2cPackets,
                                   Object2IntMap<Class<? extends S2CPacket>> s2cTypes,
                                   List<Function<FriendlyByteBuf, C2SPacket>> c2sPackets,
                                   Object2IntMap<Class<? extends C2SPacket>> c2sTypes) {
        return new PacketSetImpl(id, version, s2cPackets, s2cTypes, c2sPackets, c2sTypes);
    }
}
