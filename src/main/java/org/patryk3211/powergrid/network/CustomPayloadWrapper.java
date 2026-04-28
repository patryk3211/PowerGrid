package org.patryk3211.powergrid.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CustomPayloadWrapper(Type<CustomPayloadWrapper> type, FriendlyByteBuf data) implements CustomPacketPayload {

    public static CustomPayloadWrapper create(ResourceLocation id, FriendlyByteBuf data) {
        return new CustomPayloadWrapper(new Type<>(id), data);
    }

    public static Type<CustomPayloadWrapper> type(ResourceLocation id) {
        return new Type<>(id);
    }

    public ResourceLocation id() {
        return type.id();
    }

    /**
     * Call this when you are done with the payload data
     */
    public void release() {
        if (data != null && data.refCnt() > 0) {
            data.release();
        }
    }

    /**
     * Returns a {@link StreamCodec} for serializing and deserializing {@link CustomPayloadWrapper} instances.
     * <p>
     * This method is intended to be used for registering the codec with Minecraft's networking system
     * when custom payload packets are sent or received. If you require codec registration for your
     * custom payloads, use this method to obtain the appropriate codec and register it as needed.
     * <p>
     * If codec registration is not required, this method can be safely ignored.
     *
     * @param id The {@link ResourceLocation} identifier for the custom payload type.
     * @return A {@link StreamCodec} for {@link CustomPayloadWrapper}.
     */
    public static StreamCodec<FriendlyByteBuf, CustomPayloadWrapper> codec(ResourceLocation id) {
        return StreamCodec.of(
                (buf, payload) -> {
                    // Write all readable bytes from the payload data buffer
                    buf.writeBytes(payload.data, payload.data.readerIndex(), payload.data.readableBytes());
                },
                (buf) -> {
                    // Read all available bytes into a new buffer
                    int readableBytes = buf.readableBytes();
                    FriendlyByteBuf data = new FriendlyByteBuf(buf.readBytes(readableBytes).asReadOnly());
                    return new CustomPayloadWrapper(new Type<>(id), data);
                }
        );
    }
}