/*
 * Copyright 2025 patryk3211
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
package org.patryk3211.powergrid.electricity.sim;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.electricity.wire.powercord.CordEntity;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.CommutatorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.InductionRotorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;

public class DebugItem extends Item {
    public DebugItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getPlayer() == null)
            return InteractionResult.FAIL;
        var world = context.getLevel();
        var behaviour = BlockEntityBehaviour.get(world, context.getClickedPos(), ElectricBehaviour.TYPE);
        if(behaviour != null) {
            GlobalElectricNetworks.inspect(behaviour, context.getPlayer());
        }
        var user = context.getPlayer();
        var be = world.getBlockEntity(context.getClickedPos());
        if(be instanceof CommutatorBlockEntity commutator) {
            var power = commutator.getPower();
            user.sendSystemMessage(Component.literal("Electrical Power: " + power + " W"));
        }
        var rotor = BlockEntityBehaviour.get(world, context.getClickedPos(), RotorBehaviour.TYPE);
        if(rotor != null) {
            var power = rotor.getControllerOrThis().power;
            user.sendSystemMessage(Component.literal("Mechanical Power: " + power + " W"));
        }
        if(be instanceof InductionRotorBlockEntity rotorBE) {
            var field = rotorBE.field;
            user.sendSystemMessage(Component.literal("Field Strength: " + field));
        }
        return InteractionResult.SUCCESS;
    }

    private static void printWire(Player user, ElectricWire wire) {
        user.sendSystemMessage(Component.literal(" - " + wire)
                .withStyle(ChatFormatting.BLUE));
        if(!user.level().isClientSide && wire instanceof TransmissionLinePart part) {
            var line = part.getLine();
            var R = line == null ? 0 : line.getResistance();
            user.sendSystemMessage(Component.literal(String.format("    %s R=%f", line, R))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public InteractionResult useOn(BaseWireEntity wire, Player user, InteractionHand hand) {
        user.sendSystemMessage(user instanceof ServerPlayer
                ? Component.literal("Server:").withStyle(ChatFormatting.GOLD)
                : Component.literal("Client:").withStyle(ChatFormatting.GREEN));

        user.sendSystemMessage(Component.literal("Endpoints:")
                .withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.BOLD));
        user.sendSystemMessage(Component.literal("  1 = " + wire.getEndpoint1())
                .withStyle(ChatFormatting.GRAY));
        user.sendSystemMessage(Component.literal("  2 = " + wire.getEndpoint2())
                .withStyle(ChatFormatting.GRAY));

        user.sendSystemMessage(Component.literal("Wires:")
                .withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.BOLD));
        if(wire instanceof WireEntity swire) {
            printWire(user, swire.getWire());
        } else if(wire instanceof CordEntity mwire) {
            printWire(user, mwire.getWire1());
            printWire(user, mwire.getWire2());
        }
        return InteractionResult.SUCCESS;
    }
}
