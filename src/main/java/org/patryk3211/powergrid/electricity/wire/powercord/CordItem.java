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

import dev.architectury.event.EventResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedDataComponents;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ISocketElectric;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.patryk3211.powergrid.electricity.wire.WireConnection;
import org.patryk3211.powergrid.electricity.wire.WireItem;
import org.patryk3211.powergrid.electricity.wire.registry.WireRegistry;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;
import org.patryk3211.powergrid.compat.sable.SableUtils;

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
        assert IWire.isCord(level, stack.getItem());
        var entry = WireRegistry.forItem(level, stack.getItem());

        float distance = (float) SableUtils.projectedDistance(level, terminal1Pos, terminal2Pos);
        if(distance > entry.maximumLength()) {
            IElectric.sendMessage(context, Lang.translate("message.connection_too_long").style(ChatFormatting.RED).component());
            return InteractionResult.FAIL;
        }

        // We round the exact distance between terminals for a more favourable item usage.
        int requiredItemCount = Math.max(Math.round(distance * entry.itemsPerMeter()), 1);
        if(!PlayerUtilities.hasEnoughItems(context.getPlayer(), stack, requiredItemCount)) {
            IElectric.sendMessage(context, Lang.translate("message.connection_missing_items").style(ChatFormatting.RED).component());
            return InteractionResult.FAIL;
        }

        if(level.isClientSide)
            return InteractionResult.SUCCESS;
        ServerLevel serverWorld = (ServerLevel) level;

        CordEntity entity;
        if(stack.getItem() instanceof CordItem cordItem) {
            entity = cordItem.factory.create(serverWorld, endpoint1, endpoint2,
                    stack.copyWithCount(requiredItemCount), null);
        } else {
            // Default factory
            entity = CordEntity.create(serverWorld, endpoint1, endpoint2, stack.copyWithCount(requiredItemCount), null);
        }

        if(context.getPlayer() != null) {
            var offItem = context.getPlayer().getOffhandItem();
            if (entry.colorable() && offItem.getItem() instanceof DyeItem dye) {
                entity.setColor(dye.getDyeColor());
            }
        }

        if(!serverWorld.tryAddFreshEntityWithPassengers(entity)) {
            PowerGrid.LOGGER.error("Failed to spawn new connection wire entity.");
            IElectric.sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
            return InteractionResult.FAIL;
        }

        if(context.getPlayer() == null || !context.getPlayer().isCreative())
            PlayerUtilities.removeItems(context.getPlayer(), stack, requiredItemCount);

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult addEndpoint(UseOnContext context, ICordEndpoint endpoint) {
        var stack = context.getItemInHand();
        var firstPoint = stack.getOrDefault(ModdedDataComponents.CONNECTION_DATA.get(), WireConnection.EMPTY).endpoint();
        if(firstPoint == null) {
            stack.set(ModdedDataComponents.CONNECTION_DATA.get(), WireConnection.of(endpoint));
            IElectric.sendMessage(context, Lang.translate("message.cord_next").style(ChatFormatting.GRAY).component());
            return InteractionResult.SUCCESS;
        } else if(firstPoint instanceof ICordEndpoint firstCordPoint) {
            // Both endpoints specified
            var result = connect(firstCordPoint, endpoint, context);
            stack.remove(ModdedDataComponents.CONNECTION_DATA.get());
            return result;
        } else {
            IElectric.sendMessage(context, Lang.translate("message.connection_failed").style(ChatFormatting.RED).component());
            stack.remove(ModdedDataComponents.CONNECTION_DATA.get());
            return InteractionResult.FAIL;
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || stack.has(ModdedDataComponents.CONNECTION_DATA.get());
    }

    @NotNull
    public static EventResult useOn(Player player, InteractionHand hand, BlockPos blockPos, Direction direction) {
        var level = player.level();
        var state = level.getBlockState(blockPos);
        var electric = IElectric.getAt(level, blockPos);
        var hit = player.pick(PlayerUtilities.getReachDistance(player) + 1.0f, 1.0f, false);
        if(!(hit instanceof BlockHitResult blockHit))
            return EventResult.pass();
        var context = new UseOnContext(player, hand, blockHit);
        if(electric != null) {
            var stack = player.getMainHandItem();
            var terminal = electric.terminalIndexAt(state, hit.getLocation().subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            if(terminal >= 0) {
                var connection = stack.getOrDefault(ModdedDataComponents.CONNECTION_DATA.get(), WireConnection.EMPTY);
                if(connection.hasHalf()) {
                    // Tag has the first half of a split cord endpoint
                    var endpointHalf = connection.half();
                    if(!(endpointHalf instanceof BlockWireEndpoint bwe))
                        return EventResult.interruptFalse();
                    var endpoint2 = new BlockWireEndpoint(blockPos, terminal);
                    var p1 = endpointHalf.getExactPosition(level);
                    var p2 = endpoint2.getExactPosition(level);
                    if(p1.distanceToSqr(p2) > 4) {
                        // 4 = 2², split cord allows 2 blocks of distance between endpoints on one end of a cord.
                        IElectric.sendMessage(context, Lang.translate("message.split_cord_too_far").style(ChatFormatting.RED).component());
                        return EventResult.interruptFalse();
                    }
                    var splitEndpoint = new SplitCordEndpoint(bwe, endpoint2);
//                    CompoundTag compoundTag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
//                    compoundTag.remove("Half");
//                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(compoundTag));
                    return EventResult.interrupt(addEndpoint(context, splitEndpoint).consumesAction());
                } else {
                    var endpoint = new BlockWireEndpoint(blockPos, terminal);
                    stack.set(ModdedDataComponents.CONNECTION_DATA.get(), connection.withHalf(endpoint));
                    IElectric.sendMessage(context, Lang.translate("message.connection_next").style(ChatFormatting.GRAY).component());
                    return EventResult.interruptTrue();
                }
            }
        }
        for(var handler : PLACEMENT_HANDLERS) {
            var result = handler.place(state, context);
            if(result.getResult() == InteractionResult.PASS)
                continue;
            if(result.getResult().consumesAction())
                return EventResult.interrupt(addEndpoint(context, result.getObject()).consumesAction());
            return EventResult.interrupt(result.getResult().consumesAction());
        }
        return EventResult.pass();
    }
}
