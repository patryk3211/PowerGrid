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
package org.patryk3211.powergrid.electricity.wire.powercord;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.wire.*;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;

public class CordItem extends WireItem {
    public CordItem(Properties settings) {
        super(settings);
    }

    private static InteractionResult connect(ICordEndpoint endpoint1, ICordEndpoint endpoint2, UseOnContext context) {
        var level = context.getLevel();

        // TODO: Reimplement these checks correctly.
//        var behaviour1 = endpoint1.getElectricBehaviour(level);
//        var behaviour2 = endpoint2.getElectricBehaviour(level);
//        if(behaviour1 == null || behaviour2 == null) {
//            IElectric.sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
//            PowerGrid.LOGGER.error("Connection failed, at least one behaviour is null");
//            return InteractionResult.FAIL;
//        }
//
//        var node1 = endpoint1.getNode(level);
//        var node2 = endpoint2.getNode(level);
//        if(node1 == null || node2 == null || node1 == node2) {
//            IElectric.sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
//            PowerGrid.LOGGER.error("Connection failed, nodes: ({}, {})", node1, node2);
//            return InteractionResult.FAIL;
//        }
//
//        // Check if there is an existing connection between these nodes.
//        if(behaviour1.hasConnection(endpoint1, endpoint2) || behaviour2.hasConnection(endpoint2, endpoint1)) {
//            IElectric.sendMessage(context, Lang.translate("message.connection_exists").style(ChatFormatting.RED).component());
//            return InteractionResult.FAIL;
//        }

        var terminal1Pos = endpoint1.getExactPosition(level);
        var terminal2Pos = endpoint2.getExactPosition(level);

        var stack = context.getItemInHand();
        assert stack.getItem() instanceof IWire;
        var item = (IWire) stack.getItem();
        var tag = stack.getTag();
        assert tag != null;

        float distance = (float) terminal1Pos.distanceTo(terminal2Pos);
        if(distance > item.getMaximumLength()) {
            IElectric.sendMessage(context, Lang.translate("message.connection_too_long").style(ChatFormatting.RED).component());
            return InteractionResult.FAIL;
        }

        // We round the exact distance between terminals for a more favourable item usage.
        int requiredItemCount = Math.max(Math.round(distance), 1);
        if(!PlayerUtilities.hasEnoughItems(context.getPlayer(), stack, requiredItemCount)) {
            IElectric.sendMessage(context, Lang.translate("message.connection_missing_items").style(ChatFormatting.RED).component());
            return InteractionResult.FAIL;
        }

        if(!HangingWireEntity.checkClearance(level, endpoint1.getExactPosition(level), endpoint2.getExactPosition(level))) {
            return InteractionResult.FAIL;
        }

        if(level.isClientSide)
            return InteractionResult.SUCCESS;
        ServerLevel serverWorld = (ServerLevel) level;

        var entity = CordEntity.create(serverWorld, endpoint1, endpoint2, new ItemStack(stack.getItemHolder(), requiredItemCount), null);

        if(context.getPlayer() != null) {
            var offItem = context.getPlayer().getOffhandItem();
            if (item.canBeColored() && offItem.getItem() instanceof DyeItem dye) {
                entity.setColor(dye.getDyeColor());
            }
        }

        if(!serverWorld.tryAddFreshEntityWithPassengers(entity)) {
            PowerGrid.LOGGER.error("Failed to spawn new connection wire entity.");
            IElectric.sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
            return InteractionResult.FAIL;
        }

        if(context.getPlayer() == null || !context.getPlayer().isCreative())
            stack.shrink(requiredItemCount);

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var electric = IElectric.getAt(context.getLevel(), context.getClickedPos());
        var state = context.getLevel().getBlockState(context.getClickedPos());
        if(electric != null) {
            var stack = context.getItemInHand();
            var pos = context.getClickedPos();
            var terminal = electric.terminalIndexAt(state, context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
            if(terminal >= 0) {
                if(stack.hasTag() && stack.getTag().contains("Half")) {
                    // Tag has the first half of a split cord endpoint
                    var endpointHalf = WireEndpointType.deserialize(stack.getTagElement("Half"));
                    if(!(endpointHalf instanceof BlockWireEndpoint bwe))
                        return InteractionResult.FAIL;
                    var splitEndpoint = new SplitCordEndpoint(bwe, new BlockWireEndpoint(pos, terminal));
                    var firstPoint = WireEndpointType.deserialize(stack.getTag());
                    if(firstPoint == null) {
                        stack.setTag(splitEndpoint.serialize());
                        IElectric.sendMessage(context, Lang.translate("message.cord_next").style(ChatFormatting.GRAY).component());
                        return InteractionResult.SUCCESS;
                    } else if(firstPoint instanceof ICordEndpoint firstCordPoint) {
                        // Both endpoints specified
                        var result = connect(firstCordPoint, splitEndpoint, context);
                        stack.setTag(null);
                        return result;
                    } else {
                        IElectric.sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
                        stack.setTag(null);
                        return InteractionResult.FAIL;
                    }
                } else {
                    var endpoint = new BlockWireEndpoint(pos, terminal);
                    var tag = endpoint.serialize();
                    stack.getOrCreateTag().put("Half", tag);
                    IElectric.sendMessage(context, Lang.translate("message.connection_next").style(ChatFormatting.GRAY).component());
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }
}
