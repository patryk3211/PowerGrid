/*
 * Copyright 2026 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.network.packets;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.electricity.info.customdisplay.CustomDisplayBehaviour;
import org.patryk3211.powergrid.network.SimplePacket;
import org.patryk3211.powergrid.utility.Unit;

import java.util.function.Supplier;

public class SetCustomDisplayC2SPacket implements SimplePacket {
    public static final int MAX_UNIT_STR_LENGTH = 16;

    private final BlockPos pos;
    private final String equation;
    private final boolean usePrefixes;
    private final Unit unit;
    private final String unitStr;

    public SetCustomDisplayC2SPacket(SmartBlockEntity be, String equation, boolean usePrefixes, Unit unit, String unitStr) {
        this.pos = be.getBlockPos();
        this.equation = equation;
        this.usePrefixes = usePrefixes;
        this.unit = unit;
        this.unitStr = unitStr;
    }

    public SetCustomDisplayC2SPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        equation = buf.readUtf();
        usePrefixes = buf.readBoolean();
        if(buf.readBoolean()) {
            unit = buf.readEnum(Unit.class);
            unitStr = null;
        } else {
            unit = null;
            unitStr = buf.readUtf(MAX_UNIT_STR_LENGTH);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(equation);
        buf.writeBoolean(usePrefixes);
        if(unit != null) {
            buf.writeBoolean(true);
            buf.writeEnum(unit);
        } else {
            buf.writeBoolean(false);
            var unitStr = this.unitStr;
            if(unitStr.length() > MAX_UNIT_STR_LENGTH)
                unitStr = unitStr.substring(0, MAX_UNIT_STR_LENGTH);
            buf.writeUtf(unitStr, MAX_UNIT_STR_LENGTH);
        }
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            var player = ctx.getPlayer();
            var level = player.level();
            var behaviour = BlockEntityBehaviour.get(level, pos, CustomDisplayBehaviour.TYPE);
            if(behaviour == null)
                return;
            behaviour.set(equation, unit, unitStr, usePrefixes);
        });
    }
}
