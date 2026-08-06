package org.patryk3211.powergrid.network.packets;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.electricity.gauge.EnergyMeterBlockEntity;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class EnergyMeterInteractionC2SPacket implements SimplePacket {
    public enum Action {
        PRECISION_WH,
        PRECISION_KWH,
        ZERO
    }

    private final Action action;
    private final BlockPos pos;

    public EnergyMeterInteractionC2SPacket(Action action, BlockPos pos) {
        this.action = action;
        this.pos = pos;
    }

    public EnergyMeterInteractionC2SPacket(FriendlyByteBuf buf) {
        action = buf.readEnum(Action.class);
        pos = buf.readBlockPos();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeBlockPos(pos);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            var player = ctx.getPlayer();
            var level = player.level();

            if(!(level.getBlockEntity(pos) instanceof EnergyMeterBlockEntity meter))
                return;
            if(!player.mayInteract(level, pos))
                return;
            switch(action) {
                case ZERO -> meter.zero();
                case PRECISION_WH -> meter.setPrecise(true);
                case PRECISION_KWH -> meter.setPrecise(false);
            }
        });
    }
}
