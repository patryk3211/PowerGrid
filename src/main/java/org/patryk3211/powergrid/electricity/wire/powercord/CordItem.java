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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ISocketElectric;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;
import org.patryk3211.powergrid.electricity.wire.WireItem;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.ArrayList;
import java.util.List;

public class CordItem extends WireItem {
    public static final List<ICordPlacementHandler> PLACEMENT_HANDLERS = new ArrayList<>();

    static {
        PLACEMENT_HANDLERS.add(new ISocketElectric.Handler());
        PLACEMENT_HANDLERS.add(new IAcceptCord.Handler());
    }

    public interface CordEntityFactory {
        CordEntity create(ServerLevel level, ICordEndpoint endpoint1, ICordEndpoint endpoint2, ItemStack items, @Nullable Float resistance);
    }

    private final CordEntityFactory factory;

    public CordItem(Properties settings) {
        this(settings, CordEntity::create);
    }

    public CordItem(Properties settings, CordEntityFactory factory) {
        super(settings);
        this.factory = factory;
    }

    private static InteractionResult connect(ICordEndpoint endpoint1, ICordEndpoint endpoint2, UseOnContext context) {
        var level = context.getLevel();

        if(!endpoint1.isValid(level) || !endpoint2.isValid(level)) {
            IElectric.sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
            PowerGrid.LOGGER.error("Connection failed, at least one endpoint is not valid");
            return InteractionResult.FAIL;
        }

        // These checks differ from the hanging wire checks.
        // We treat the cord as if it were two regular wires in parallel,
        // which means that we allow some permutations where the individual wires go into one terminal.
        var node1 = endpoint1.getEndpoint1().getNode(level);
        var node2 = endpoint2.getEndpoint1().getNode(level);
        if(node1 == null || node2 == null || node1 == node2) {
            IElectric.sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
            PowerGrid.LOGGER.error("Connection failed, nodes: ({}, {})", node1, node2);
            return InteractionResult.FAIL;
        }
        node1 = endpoint1.getEndpoint2().getNode(level);
        node2 = endpoint2.getEndpoint2().getNode(level);
        if(node1 == null || node2 == null || node1 == node2) {
            IElectric.sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
            PowerGrid.LOGGER.error("Connection failed, nodes: ({}, {})", node1, node2);
            return InteractionResult.FAIL;
        }

        var e11 = endpoint1.getEndpoint1();
        var e21 = endpoint2.getEndpoint1();
        if(e11 instanceof BlockWireEndpoint be1 && e21 instanceof BlockWireEndpoint be2) {
            var behaviour1 = be1.getElectricBehaviour(level);
            var behaviour2 = be2.getElectricBehaviour(level);
            // Check if there is an existing connection between these nodes.
            if (behaviour1.hasConnection(be1, be2) || behaviour2.hasConnection(be2, be1)) {
                IElectric.sendMessage(context, Lang.translate("message.connection_exists").style(ChatFormatting.RED).component());
                return InteractionResult.FAIL;
            }
        }
        var e12 = endpoint1.getEndpoint2();
        var e22 = endpoint2.getEndpoint2();
        if(e12 instanceof BlockWireEndpoint be1 && e22 instanceof BlockWireEndpoint be2) {
            var behaviour1 = be1.getElectricBehaviour(level);
            var behaviour2 = be2.getElectricBehaviour(level);
            // Check if there is an existing connection between these nodes.
            if (behaviour1.hasConnection(be1, be2) || behaviour2.hasConnection(be2, be1)) {
                IElectric.sendMessage(context, Lang.translate("message.connection_exists").style(ChatFormatting.RED).component());
                return InteractionResult.FAIL;
            }
        }

        var terminal1Pos = endpoint1.getExactPosition(level);
        var terminal2Pos = endpoint2.getExactPosition(level);

        var stack = context.getItemInHand();
        assert stack.getItem() instanceof CordItem;
        var item = (CordItem) stack.getItem();

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

        if(level.isClientSide)
            return InteractionResult.SUCCESS;
        ServerLevel serverWorld = (ServerLevel) level;

        var entity = item.factory.create(serverWorld, endpoint1, endpoint2,
                stack.copyWithCount(requiredItemCount), null);

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

    private static InteractionResult addEndpoint(UseOnContext context, ICordEndpoint endpoint) {
        var stack = context.getItemInHand();
        var firstPoint = WireEndpointType.deserialize(stack.getTagElement("Connection"));
        if(firstPoint == null) {
            stack.getOrCreateTag().put("Connection", endpoint.serialize());
            IElectric.sendMessage(context, Lang.translate("message.cord_next").style(ChatFormatting.GRAY).component());
            return InteractionResult.SUCCESS;
        } else if(firstPoint instanceof ICordEndpoint firstCordPoint) {
            // Both endpoints specified
            var result = connect(firstCordPoint, endpoint, context);
            stack.removeTagKey("Connection");
            return result;
        } else {
            IElectric.sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
            stack.removeTagKey("Connection");
            return InteractionResult.FAIL;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        if(stack.hasTag() && user.isShiftKeyDown()) {
            stack.removeTagKey("Connection");
            stack.removeTagKey("Half");
            if(!world.isClientSide)
                user.displayClientMessage(Lang.translate("message.connection_reset").style(ChatFormatting.GRAY).component(), true);
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || (stack.hasTag() && stack.getTag().contains("Half"));
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        var state = context.getLevel().getBlockState(context.getClickedPos());
        var electric = IElectric.getAt(context.getLevel(), context.getClickedPos());
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
                    var endpoint2 = new BlockWireEndpoint(pos, terminal);
                    var p1 = endpointHalf.getExactPosition(context.getLevel());
                    var p2 = endpoint2.getExactPosition(context.getLevel());
                    if(p1.distanceToSqr(p2) > 4) {
                        // 4 = 2², split cord allows 2 blocks of distance between endpoints on one end of a cord.
                        IElectric.sendMessage(context, Lang.translate("message.split_cord_too_far").style(ChatFormatting.RED).component());
                        return InteractionResult.FAIL;
                    }
                    var splitEndpoint = new SplitCordEndpoint(bwe, endpoint2);
                    stack.removeTagKey("Half");
                    return addEndpoint(context, splitEndpoint);
                } else {
                    var endpoint = new BlockWireEndpoint(pos, terminal);
                    var tag = endpoint.serialize();
                    stack.getOrCreateTag().put("Half", tag);
                    IElectric.sendMessage(context, Lang.translate("message.connection_next").style(ChatFormatting.GRAY).component());
                    return InteractionResult.SUCCESS;
                }
            }
        }
        for(var handler : PLACEMENT_HANDLERS) {
            var result = handler.place(state, context);
            if(result.getResult() == InteractionResult.PASS)
                continue;
            if(result.getResult().consumesAction())
                return addEndpoint(context, result.getObject());
            return result.getResult();
        }
        return InteractionResult.PASS;
    }
}
