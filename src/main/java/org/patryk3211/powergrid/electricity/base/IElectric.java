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
package org.patryk3211.powergrid.electricity.base;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.wire.*;
import org.patryk3211.powergrid.electricity.wire.registry.WireRegistry;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;

public interface IElectric extends IWrenchable {
    /**
     * Get terminal index located at the given position.
     * @param pos Position inside the block.
     * @return Terminal index as defined in the block entity, -1 if there is no terminal at this position.
     */
    default int terminalIndexAt(BlockState state, Vec3 pos) {
        for(int i = 0; i < terminalCount(); ++i) {
            var terminal = terminal(state, i);
            if(terminal == null)
                continue;
            if(terminal.check(pos))
                return i;
        }
        return -1;
    }

    default ITerminalPlacement terminalAt(BlockState state, Vec3 pos) {
        for(int i = 0; i < terminalCount(); ++i) {
            var terminal = terminal(state, i);
            if(terminal == null)
                continue;
            if(terminal.check(pos))
                return terminal;
        }
        return null;
    }

    int terminalCount();

    default boolean accepts(ItemStack wireStack) {
        // By default, only light wires can go directly to devices.
        return wireStack.is(ModdedTags.Item.LIGHT_WIRES.tag);
    }

    /**
     * Get the terminal placement of a given terminal
     * @param state Block state
     * @param index Terminal index
     * @return Terminal placement
     */
    ITerminalPlacement terminal(BlockState state, int index);

    default InteractionResult onWire(BlockState state, UseOnContext context) {
        var stack = context.getItemInHand();
        var pos = context.getClickedPos();
        var terminal = terminalIndexAt(state, context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
        if(terminal >= 0) {
            if(!accepts(context.getItemInHand())) {
                sendMessage(context, Lang.translate("message.connection_incorrect_wire_type").style(ChatFormatting.RED).component());
                return InteractionResult.FAIL;
            }
            if(stack.hasTag() && stack.getTag().contains("Connection")) {
                // Continuing a connection.
                var endpoint = WireEndpointType.deserialize(stack.getTagElement("Connection"));
                if(endpoint == null)
                    return InteractionResult.FAIL;
                var result = makeConnection(context.getLevel(), endpoint, new BlockWireEndpoint(pos, terminal), context);
                if(result.consumesAction())
                    stack.removeTagKey("Connection");
                return result;
            } else {
                // Must be first connection.
                var endpoint = new BlockWireEndpoint(pos, terminal);
                var tag = endpoint.serialize();
                stack.getOrCreateTag().put("Connection", tag);
                sendMessage(context, Lang.translate("message.connection_next").style(ChatFormatting.GRAY).component());
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Nullable
    default ElectricBehaviour getBehaviour(Level world, BlockPos pos, BlockState state) {
        var blockEntity = world.getBlockEntity(pos);
        if(blockEntity instanceof SmartBlockEntity smartEntity)
            return smartEntity.getBehaviour(ElectricBehaviour.TYPE);
        return null;
    }

    @Nullable
    static IElectric getAt(Level world, BlockPos pos) {
        var be = world.getBlockEntity(pos);
        if(be instanceof IElectric electric)
            return electric;
        var state = world.getBlockState(pos);
        if(state.getBlock() instanceof IElectric electric)
            return electric;
        return null;
    }

    @NotNull
    static Vec3 getTerminalPos(Level world, BlockPos position, int terminalIndex) {
        var electric = getAt(world, position);
        if(electric == null)
            return position.getCenter();
        var state = world.getBlockState(position);
        var terminal = electric.terminal(state, terminalIndex);
        if(terminal == null)
            return position.getCenter();
        var origin = terminal.getOrigin();
        return new Vec3(position.getX() + origin.x, position.getY() + origin.y, position.getZ() + origin.z);
    }

    static void sendMessage(UseOnContext context, Component text) {
        if(context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(text, true);
        }
    }

    static InteractionResult makeConnection(Level world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, UseOnContext context) {
        if(endpoint1.type() == WireEndpointType.BLOCK && endpoint2.type() == WireEndpointType.BLOCK) {
            // Hanging wire connection.
            return makeHangingWireConnection(world, (BlockWireEndpoint) endpoint1, (BlockWireEndpoint) endpoint2, context);
        }

        var result = WireItem.connect(world, context.getItemInHand(), context.getPlayer(), endpoint1, endpoint2);
        return result.getResult();
    }

    static InteractionResult makeHangingWireConnection(Level world, BlockWireEndpoint endpoint1, BlockWireEndpoint endpoint2, UseOnContext context) {
        var behaviour1 = endpoint1.getElectricBehaviour(world);
        var behaviour2 = endpoint2.getElectricBehaviour(world);
        if(behaviour1 == null || behaviour2 == null) {
            sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
            PowerGrid.LOGGER.error("Connection failed, at least one behaviour is null");
            return InteractionResult.FAIL;
        }

        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);
        if(node1 == null || node2 == null || node1 == node2) {
            sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
            PowerGrid.LOGGER.error("Connection failed, nodes: ({}, {})", node1, node2);
            return InteractionResult.FAIL;
        }

        // Check if there is an existing connection between these nodes.
        if(behaviour1.hasConnection(endpoint1, endpoint2) || behaviour2.hasConnection(endpoint2, endpoint1)) {
            sendMessage(context, Lang.translate("message.connection_exists").style(ChatFormatting.RED).component());
            return InteractionResult.FAIL;
        }

        var terminal1Pos = endpoint1.getExactPosition(world);
        var terminal2Pos = endpoint2.getExactPosition(world);

        var stack = context.getItemInHand();
        assert IWire.isWire(world, stack.getItem());
        var entry = WireRegistry.forItem(world, stack.getItem());

        float distance = (float) terminal1Pos.distanceTo(terminal2Pos);
        if(distance > entry.maximumLength()) {
            sendMessage(context, Lang.translate("message.connection_too_long").style(ChatFormatting.RED).component());
            return InteractionResult.FAIL;
        }

        // We round the exact distance between terminals for a more favourable item usage.
        int requiredItemCount = Math.max(Math.round(distance * entry.itemsPerMeter()), 1);
        if(!PlayerUtilities.hasEnoughItems(context.getPlayer(), stack, requiredItemCount)) {
            sendMessage(context, Lang.translate("message.connection_missing_items").style(ChatFormatting.RED).component());
            return InteractionResult.FAIL;
        }

        if(!HangingWireEntity.checkClearance(world, endpoint1.getExactPosition(world), endpoint2.getExactPosition(world))) {
            return InteractionResult.FAIL;
        }

        if(world.isClientSide)
            return InteractionResult.SUCCESS;
        ServerLevel serverWorld = (ServerLevel) world;

        // The amount of used items dictates the resistance of a connection,
        // to make sure everything is fair.
        var entity = HangingWireEntity.create(serverWorld, endpoint1, endpoint2, new ItemStack(stack.getItemHolder(), requiredItemCount), null);

        if(context.getPlayer() != null) {
            var offItem = context.getPlayer().getOffhandItem();
            if (entry.colorable() && offItem.getItem() instanceof DyeItem dye) {
                entity.setColor(dye.getDyeColor());
            }
        }

        if(!serverWorld.tryAddFreshEntityWithPassengers(entity)) {
            PowerGrid.LOGGER.error("Failed to spawn new connection wire entity.");
            sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
            return InteractionResult.FAIL;
        }

        if(context.getPlayer() == null || !context.getPlayer().isCreative())
            PlayerUtilities.removeItems(context.getPlayer(), stack, requiredItemCount);

        return InteractionResult.SUCCESS;
    }
}
